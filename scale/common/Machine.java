package scale.common;

/**
 * This is the base class for all machine specific information.
 * <p>
 * $Id: Machine.java,v 1.61 2007-11-01 16:52:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class Machine
{
  /**
   * Capability: Has a conditional move using an integer test and
   * integer values.
   */
  public static final int HAS_II_CONDITIONAL_MOVE = 1; 
  /**
   * Capability: Has a conditional move using an integer test and
   * floating point values.
   */
  public static final int HAS_IF_CONDITIONAL_MOVE = 2; 
  /**
   * Capability: Has a conditional move using a floating point test
   * and integer values.
   */
  public static final int HAS_FI_CONDITIONAL_MOVE = 4; 
  /**
   * Capability: Has a conditional move using a floating point test
   * and floating point values.
   */
  public static final int HAS_FF_CONDITIONAL_MOVE = 8;
  /**
   * Capability: Has a floating point compare that returns an integer
   * value.
   */
  public static final int HAS_INT_FROM_FP_CMP = 16;
  /**
   * Capability: Has a simple inline floor capability.
   */
  public static final int HAS_SIMPLE_FLOOR = 32;
  /**
   * Capability: Has a floating point registers that are saved over
   * function calls.
   */
  public static final int HAS_NON_VOLATILE_FP_REGS = 64;
  /**
   * Capability: Has predicated instructions.
   */
  public static final int HAS_PREDICATION = 128;
  /**
   * Capability: Has benefit from unrolling loops with no induction
   * variable.
   */
  public static final int HAS_WHILE_LOOP_UNROLL_BENEFIT= 256;
  /**
   * Capability: bad to flatten/unroll loops that contain a function
   * call.
   */
  public static final int HAS_EXPENSIVE_SUBROUTINE_CALLS = 512;
  /**
   * Capability: Integer divides are not performed by an instruction
   * but by a subroutine call.
   */
  public static final int HAS_NO_INT_DIVIDE = 1024;
  /**
   * Capability: Floating point divides are not performed by an
   * instruction but by a subroutine call.
   */
  public static final int HAS_NO_FP_DIVIDE = 2048;
  /**
   * Capability: All the conditional move possibilities.
   */
  public static final int ALL_CONDITIONAL_MOVES = (HAS_II_CONDITIONAL_MOVE |
                                                   HAS_IF_CONDITIONAL_MOVE |
                                                   HAS_FI_CONDITIONAL_MOVE |
                                                   HAS_FF_CONDITIONAL_MOVE);

  /**
   * The current machine specification.
   */
  public static Machine currentMachine = null;
  /**
   * The current machine assembly language generator constructor.
   */
  private static java.lang.reflect.Constructor generator = null;
  /**
   * The size in addressable units of an L1 cache line.
   */
  protected int cacheLineSize = 32;
  /**
   * The type for the smallest addressable unit (e.g., char).
   */
  protected scale.clef.type.IntegerType smallestAddressableUnitType;
  /**
   * The integer type best used for integer calculations.
   */
  protected scale.clef.type.IntegerType intCalcType;
  /**
   * The real type best used for floating point calculations.
   */
  protected scale.clef.type.FloatType floatCalcType;
  /**
   * The type to be used for <code>va_list</code>.
   */
  protected scale.clef.type.Type vaListType;
  /**
   * The type used for the C <code>signed char</code> type.
   */
  protected scale.clef.type.IntegerType signedCharType;
  /**
   * The type used for the C <code>unsigned char</code> type.
   */
  protected scale.clef.type.IntegerType unsignedCharType;
  /**
   * The type used for the C <code>signed short</code> type.
   */
  protected scale.clef.type.IntegerType signedShortType;
  /**
   * The type used for the C <code>unsigned short</code> type.
   */
  protected scale.clef.type.IntegerType unsignedShortType;
  /**
   * The type used for the C <code>signed int</code> type.
   */
  protected scale.clef.type.IntegerType signedIntType;
  /**
   * The type used for the C <code>unsigned int</code> type.
   */
  protected scale.clef.type.IntegerType unsignedIntType;
  /**
   * The type used for the C <code>signed long</code> type.
   */
  protected scale.clef.type.IntegerType signedLongType;
  /**
   * The type used for the C <code>unsigned long</code> type.
   */
  protected scale.clef.type.IntegerType unsignedLongType;
  /**
   * The type used for the C <code>signed long long</code> type.
   */
  protected scale.clef.type.IntegerType signedLongLongType;
  /**
   * The type used for the C <code>unsigned long long</code> type.
   */
  protected scale.clef.type.IntegerType unsignedLongLongType;
  /**
   * The type used for the C <code>size_t</code> type.
   */
  protected scale.clef.type.IntegerType sizetType;
  /**
   * The type used for the C <code>ptrdiff_t</code> type.
   */
  protected scale.clef.type.IntegerType ptrdifftType;
  /**
   * The type used for the C <code>char_t</code> type.
   */
  protected scale.clef.type.IntegerType wchartType;
  /**
   * The type used for the C <code>void*</code> type.
   */
  protected scale.clef.type.PointerType voidStarType;
  /**
   * The type used for the C <code>float</code> type.
   */
  protected scale.clef.type.FloatType floatType;
  /**
   * The type used for the C <code>double</code> type.
   */
  protected scale.clef.type.FloatType doubleType;
  /**
   * The type used for the C <code>long double</code> type.
   */
  protected scale.clef.type.FloatType longDoubleType;

  private static Object[] backendArgs = new Object[3];

  /**
   * Certain optimizations or representations are only useful if the
   * architecture has support for them.
   */
  private int capabilities = 0;

  /**
   * Create a Machine instance.
   * @param capabilities specifies special abilities of this
   * architecture that the compiler can use
   */
  public Machine(int capabilities)
  {
    this.capabilities = capabilities;
  }

  /**
   * Return true if this architecture has one of the specified
   * capabilities.
   */
  public final boolean hasCapability(int capability)
  {
    return (this.capabilities & capability) != 0;
  }

  /**
   * Return true if this architecture has all of the specified
   * capabilities.
   */
  public final boolean hasCapabilities(int capabilities)
  {
    return (this.capabilities & capabilities) == capabilities;
  }

  /**
   * Return the type of the smallest addressable unit for this
   * machine.  For most machines this is type <code>char</code>.
   */
  public final scale.clef.type.IntegerType getSmallestAddressableUnitType()
  {
    return smallestAddressableUnitType;
  }

  /**
   * Return the type used for the C <code>signed char</code> type.
   */
  public final scale.clef.type.IntegerType getSignedCharType()
  {
    return signedCharType;
  }

  /**
   * Return the type used for the C <code>unsigned char</code> type.
   */
  public final scale.clef.type.IntegerType getUnsignedCharType()
  {
    return unsignedCharType;
  }

  /**
   * Return the type used for the C <code>signed short</code> type.
   */
  public final scale.clef.type.IntegerType getSignedShortType()
  {
    return signedShortType;
  }

  /**
   * Return the type used for the C <code>unsigned short</code> type.
   */
  public final scale.clef.type.IntegerType getUnsignedShortType()
  {
    return unsignedShortType;
  }

  /**
   * Return the type used for the C <code>signed int</code> type.
   */
  public final scale.clef.type.IntegerType getSignedIntType()
  {
    return signedIntType;
  }

  /**
   * Return the type used for the C <code>unsigned int</code> type.
   */
  public final scale.clef.type.IntegerType getUnsignedIntType()
  {
    return unsignedIntType;
  }

  /**
   * Return the type used for the C <code>signed long</code> type.
   */
  public final scale.clef.type.IntegerType getSignedLongType()
  {
    return signedLongType;
  }

  /**
   * Return the type used for the C <code>unsigned long</code> type.
   */
  public final scale.clef.type.IntegerType getUnsignedLongType()
  {
    return unsignedLongType;
  }

  /**
   * Return the type used for the C <code>signed long long</code>
   * type.
   */
  public final scale.clef.type.IntegerType getSignedLongLongType()
  {
    return signedLongLongType;
  }

  /**
   * Return the type used for the C <code>unsigned long long</code>
   * type.
   */
  public final scale.clef.type.IntegerType getUnsignedLongLongType()
  {
    return unsignedLongLongType;
  }

  /**
   * Return the type used for the C <code>size_t</code> type.
   */
  public final scale.clef.type.IntegerType getSizetType()
  {
    return sizetType;
  }

  /**
   * Return the type used for the C <code>ptrdiff_t</code> type.
   */
  public final scale.clef.type.IntegerType getPtrdifftType()
  {
    return ptrdifftType;
  }

  /**
   * Return the type used for the C <code>wchar_t</code> type.
   */
  public final scale.clef.type.IntegerType getWchartType()
  {
    return wchartType;
  }

  /**
   * Return the type used for the C <code>void*</code> type.
   */
  public final scale.clef.type.PointerType getVoidStarType()
  {
    return voidStarType;
  }

  /**
   * Return the type used for the C <code>float</code> type.
   */
  public final scale.clef.type.FloatType getFloatType()
  {
    return floatType;
  }

  /**
   * Return the type used for the C <code>double</code> type.
   */
  public final scale.clef.type.FloatType getDoubleType()
  {
    return doubleType;
  }

  /**
   * Return the type used for the C <code>long double</code> type.
   */
  public final scale.clef.type.FloatType getLongDoubleType()
  {
    return longDoubleType;
  }

  /**
   * Return the type best used for integer calculations.  On the Alpha
   * this is a signed 64-bit integer type while on the Sparc V8 it is
   * a signed 32-bit integer type.
   */
  public final scale.clef.type.IntegerType getIntegerCalcType()
  {
    return intCalcType;
  }

  /**
   * Return the type best used for floating point calculations.
   * On Trips this is a signed 64-bit real type.
   */
  public final scale.clef.type.FloatType getRealCalcType()
  {
    return floatCalcType;
  }

  /**
   * Return the type to be used for <code>va_list</code>.
   */
  public final scale.clef.type.Type getVaListType()
  {
    return vaListType;
  }

  /**
   * Setup this instance of the machine.  Each code generator should
   * implement this method.  For example
   * <pre>
   *   protected void setup()
   *   {
   *     super.setup(); // Initialize type, etc that are not set here.
   * 
   *     // Specify the types that are different.
   * 
   *     PointerType.setMinBitSize(64);
   *     intCalcType = scale.clef.type.SignedIntegerType.create(64);
   *     ....
   *   }
   * </pre>
   */
  protected void setup()
  {
    // Specify types.

    scale.clef.type.PointerType.setMinBitSize(32);

    smallestAddressableUnitType = scale.clef.type.SignedIntegerType.create(8);
    intCalcType                 = scale.clef.type.SignedIntegerType.create(32);
    floatCalcType               = scale.clef.type.FloatType.create(32);
    signedCharType              = scale.clef.type.SignedIntegerType.create(8);
    unsignedCharType            = scale.clef.type.UnsignedIntegerType.create(8);
    wchartType                  = scale.clef.type.UnsignedIntegerType.create(16);
    signedShortType             = scale.clef.type.SignedIntegerType.create(16);
    unsignedShortType           = scale.clef.type.UnsignedIntegerType.create(16);
    signedIntType               = scale.clef.type.SignedIntegerType.create(32);
    unsignedIntType             = scale.clef.type.UnsignedIntegerType.create(32);
    signedLongType              = scale.clef.type.SignedIntegerType.create(32);
    unsignedLongType            = scale.clef.type.UnsignedIntegerType.create(32);
    signedLongLongType          = scale.clef.type.SignedIntegerType.create(64);
    unsignedLongLongType        = scale.clef.type.UnsignedIntegerType.create(64);
    sizetType                   = scale.clef.type.UnsignedIntegerType.create(32);
    ptrdifftType                = scale.clef.type.SignedIntegerType.create(32);
    floatType                   = scale.clef.type.FloatType.create(32);
    doubleType                  = scale.clef.type.FloatType.create(64);
    longDoubleType              = scale.clef.type.FloatType.create(64);

    scale.clef.type.Type vt = scale.clef.type.VoidType.type;

    voidStarType                = scale.clef.type.PointerType.create(vt);
    vaListType                  = scale.clef.type.PointerType.create(vt);

    // Specify other things.

    cacheLineSize = 16;
  }

  /**
   * Return the integer value <tt>alignment</tt>that satisfies 
   * <code>(0 == address % alignment)</code>
   * for the data size specified.
   * @param dataSize is the size of the data in addressable units.
   */
  public abstract int alignData(int dataSize);
  /**
   * Return the number of addressable units required
   * @param bitSize is the number of bits required for the data
   */
  public abstract int addressableMemoryUnits(int bitSize);
  /**
   * Return an estimate of the execution cost to provide this value.
   * The cost should be zero if the value can be represented as a
   * hard-wired register or in the immediate field of an instruction.
   * The cost is relative to the cost of other operations; a good
   * metric is the number of cycles needed to generate the value or
   * load it from memory.
   */
  public abstract int executionCostEstimate(long value);
  /**
   * Return an estimate of the execution cost to provide this value.
   * The cost should be zero if the value can be represented as a
   * hard-wired register or in the immediate field of an instruction.
   * The cost is relative to the cost of other operations; a good
   * metric is the number of cycles needed to generate the value or
   * load it from memory.
   */
  public abstract int executionCostEstimate(double value);

  /**
   * Return the maximum bit field size in bits.
   * Also, bit fields can not be split over this boundary.
   */
  public int maxBitFieldSize()
  {
    return 32;
  }

  /**
   * Round up a value so that it is evenly divisible by the second
   * value.
   * @param value - returned rounded up
   * @param to - specifies the divisor
   * @return value rounded up 
   */
  public static long alignTo(long value, int to)
  {
    long k = value % to;
    return value + ((k == 0) ? 0 : (to - k));
  }

  /**
   * Add the flags necessary for the C preprocessor.
   */
  public abstract void addCPPFlags(Vector<String> v);
  /**
   * Return the most general purpose alignment in memory units.
   */
  public abstract int generalAlignment();
  /**
   * Return the alignment of things stored on the stack.
   */
  public abstract int stackAlignment(scale.clef.type.Type type);
  /**
   * Return true if the machine is little-endian.
   */
  public abstract boolean littleEndian();

  /**
   * Return the number of elements of the specified size in a cache
   * line.  Using a value of 1 returns the cache line size in
   * addressable units.
   * @param typeSize is the number of addressable units per element
   */
  public int getCacheSize(int typeSize)
  {
    return cacheLineSize / typeSize;
  }

  /**
   * Return the name of the generic target architecture.
   */
  public abstract String getGenericArchitectureName();

  /**
   * Return the name of the specific target architecture.
   */
  public abstract String getArchitectureName();

  /**
   * Determine the architecture sub-type.
   * @param architecture specifies the target architecture
   * @param extension specifies an extension to the a target architecture
   * @return the name of the specific target architecture generator
   * class.
   * @throws java.lang.Exception if the extension is not understood
   */
  public abstract String determineArchitecture(String architecture,
                                               String extension) throws java.lang.Exception;

  /**
   * Return the file extension to use for an assembler source file.
   */
  public String getAsmFileExtension()
  {
    return ".s";
  }
  
  private static String changeName(String name)
  {
    char[] n = name.toCharArray();
    n[0] = Character.toUpperCase(n[0]);
    return new String(n);
  }

  /**
   * Setup for the specified machine.
   * The currentMachine is initialized and the
   * assembly generator class is located.
   */
  public static String setup(String architecture)
  {
    int    sl        = architecture.indexOf('/');
    String extension = ""; // Architecture modifier

    if (sl > 0) {
      extension = architecture.substring(sl + 1);
      architecture = architecture.substring(0, sl);
    }

    String packageM = architecture;
    String classM   = architecture;

    if (architecture.startsWith("sparc")) {
      classM = "sparc";
      packageM = "sparc";
    } else if (architecture.startsWith("ppc")) {
      classM = "PPC";
      packageM = "ppc";
    } else if (architecture.endsWith("86")) {
      classM = "X86";
      packageM = "x86";
    }

    classM = changeName(classM);

    String nameM = "scale.backend." + packageM + "." + classM + "Machine";
    String nameG;

    // Determine Machine class for the specified architecture.

    try {
      Class[]  argNamesm = new Class[0];
      Object[] argsm     = new Object[0];
      Class<?> opClass   = Class.forName(nameM);

      java.lang.reflect.Constructor cnstr = opClass.getDeclaredConstructor(argNamesm);
      currentMachine = (Machine) cnstr.newInstance(argsm);
      nameG = currentMachine.determineArchitecture(architecture, extension);
      currentMachine.setup();
    } catch (java.lang.Exception ex) {
      Msg.reportError(Msg.MSG_Machine_s_not_found, null, 0, 0, nameM);
      return null;
    }

    // Obtain the constructor for the generator class.

    try {
      Class[] argNames = new Class[3];
      argNames[0] = Class.forName("scale.callGraph.CallGraph");
      argNames[1] = Class.forName("scale.common.Machine");
      argNames[2] = Integer.TYPE; // Various flags
      Class<?> genc = Class.forName(nameG);
      generator = genc.getDeclaredConstructor(argNames);
    } catch (java.lang.Exception ex) {
      Msg.reportError(Msg.MSG_s, null, 0, 0, ex.getMessage());
      return null;
    }

    return architecture;
  }

  /**
   * Return a backend code generator instance for the specified target
   * architecture.
   * @param cg is the call graph containing the CFGs to be converted
   * to target architecture instructions
   * @param backendFeatures specifies certain flags set on the command
   * line
   */
  public static scale.backend.Generator getCodeGenerator(scale.callGraph.CallGraph cg,
                                                         int backendFeatures)
    throws java.lang.Exception
  {
    backendArgs[0] = cg;
    backendArgs[1] = Machine.currentMachine;
    backendArgs[2] = new Integer(backendFeatures);

    scale.backend.Generator gen = (scale.backend.Generator) generator.newInstance(backendArgs);

    backendArgs[0] = null; // Let it be garbage collected if it is not used.
    backendArgs[1] = null;
    backendArgs[2] = null;

    return gen;
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
   * Return the assembler command to use.
   * If a user-specified value is available, use it.
   * Otherwise generate one based on the architecture.
   * @param user is the command specified by the user.
   * @param backendFeatures specifies code generation flags such as -g
   */
  public static String getAssemblerCommand(String user, int backendFeatures)
  {
    if (!"as".equals(user))
      return user;
    return currentMachine.getAssemblerCommand(backendFeatures);
  }

  /**
   * Return true if a value of the type should be allocated to a
   * register.  If it is volatile or not an atomic type, it probably
   * should not be kept in a register.
   * @param type is the type
   * @param temporary is true if the duration will be short
   */
  public boolean keepTypeInRegister(scale.clef.type.Type type, boolean temporary)
  {
    if (type.isVolatile())
      return false;

    scale.clef.type.Type ctype = type.getCoreType();
    if (ctype.isAtomicType())
      return true;

    if (!ctype.isAggregateType()) {
      if (ctype instanceof scale.clef.type.FortranCharType) {
        scale.clef.type.FortranCharType fct = (scale.clef.type.FortranCharType) type;
        return fct.getLength() <= 4;
      }
      return false;
    }

    long bs = ctype.memorySize(this);
    if (bs > 16)
      return false;

    int ft = ((scale.clef.type.AggregateType) ctype).allFieldsType();

    return (ft == scale.clef.type.AggregateType.FT_INT) ||
           (ft == scale.clef.type.AggregateType.FT_F32) ||
           (ft == scale.clef.type.AggregateType.FT_F64);
  }

  /**
   * Return the proper instruction count estimator for the target
   * architecture.
   */
  public scale.backend.ICEstimator getInstructionCountEstimator()
  {
    return new scale.backend.ICEstimator(currentMachine);
  }

  /**
   * Return the proper instruction count estimator for the target
   * architecture.
   */
  public static scale.backend.ICEstimator sGetInstructionCountEstimator()
  {
    return currentMachine.getInstructionCountEstimator();
  }

  /**
   * For architectures that block instructions, this is the maximum
   * number of instructions that can be in a block.  For other
   * architectures, the value 0 is returned.
   */
  public int getMaxBlockSize()
  {
    return 64;
  }

  /**
   * Return the functional unit information.  This information is an
   * array where each element of the array is the number of
   * instructions that can be issued to the corresponding functional
   * unit.  This method should be overridden in each architecture.
   * @see scale.backend.Instruction#getFunctionalUnit
   */
  public byte[] getFunctionalUnitDescriptions()
  {
    return functionalUnits;
  }

  private static final byte[] functionalUnits = {100};
}
