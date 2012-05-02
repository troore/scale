package scale.backend.xyz;

import scale.common.*;
import scale.clef.type.*;
import scale.clef.decl.FieldDecl;
import scale.backend.Generator;

/**
 * This is the base class for all Xyz specific information.
 * <p>
 * $Id: XyzMachine.java,v 1.4 2007-11-01 16:52:30 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is used by the compiler to determine architecture
 * specific information.  You must modify each method to provide the
 * information specific to your system.  Some of the methods below are
 * preset for a 64-bit system and may need to be changed for a 32-bit
 * system.  Some of the methods may be removed if the default in the
 * base class is suitable.  Use the machine classes from the other
 * architectures to help you understand how to make changes for your
 * architecture.
 * @see scale.common.Machine
 * @see scale.backend.alpha.AlphaMachine
 * @see scale.backend.sparc.SparcMachine
 * @see scale.backend.ppc.PPCMachine
 */
public class XyzMachine extends Machine
{
  private static final int capabilities = 0;

  /**
   * Create a Machine instance for an Xyz.
   */
  public XyzMachine()
  {
    super(capabilities);

    scale.clef.type.PointerType.setMinBitSize(64);
  }

  protected void setup()
  {
    super.setup();

    // Specify the types that are different.

//     scale.clef.type.PointerType.setMinBitSize(32);

//     smallestAddressableUnitType = scale.clef.type.SignedIntegerType.create(8);
//     intCalcType                 = scale.clef.type.SignedIntegerType.create(32);
//     floatCalcType               = scale.clef.type.FloatType.create(32);
//     vaListType                  = scale.clef.type.PointerType.create(scale.clef.type.VoidType.type);
//     signedCharType              = scale.clef.type.SignedIntegerType.create(8);
//     unsignedCharType            = scale.clef.type.UnsignedIntegerType.create(8);
//     wchartType                  = scale.clef.type.UnsignedIntegerType.create(16);
//     signedShortType             = scale.clef.type.SignedIntegerType.create(16);
//     unsignedShortType           = scale.clef.type.UnsignedIntegerType.create(16);
//     signedIntType               = scale.clef.type.SignedIntegerType.create(32);
//     unsignedIntType             = scale.clef.type.UnsignedIntegerType.create(32);
//     signedLongType              = scale.clef.type.SignedIntegerType.create(32);
//     unsignedLongType            = scale.clef.type.UnsignedIntegerType.create(32);
//     unsignedLongLongType        = scale.clef.type.UnsignedIntegerType.create(64);
//     sizetType                   = scale.clef.type.UnsignedIntegerType.create(32);
//     ptrdifftType                = scale.clef.type.SignedIntegerType.create(32);
//     voidStarType                = scale.clef.type.PointerType.create(scale.clef.type.VoidType.type);
//     floatType                   = scale.clef.type.FloatType.create(32);
//     doubleType                  = scale.clef.type.FloatType.create(64);
//     longDoubleType              = scale.clef.type.FloatType.create(64);

    // Specify other things.

//     cacheLineSize = 16;
  }

  protected void setUnsignedCharType()
  {
    unsignedCharType = scale.clef.type.UnsignedIntegerType.create(8);
  }

  protected void setSignedShortType()
  {
    signedShortType = scale.clef.type.SignedIntegerType.create(16);
  }

  protected void setUnsignedShortType()
  {
    unsignedShortType = scale.clef.type.UnsignedIntegerType.create(16);
  }

  protected void setSignedIntType()
  {
    signedIntType = scale.clef.type.SignedIntegerType.create(32);
  }

  protected void setUnsignedIntType()
  {
    unsignedIntType = scale.clef.type.UnsignedIntegerType.create(32);
  }

  protected void setSignedLongType()
  {
    signedLongType = scale.clef.type.SignedIntegerType.create(32);
  }

  protected void setUnsignedLongType()
  {
    unsignedLongType = scale.clef.type.UnsignedIntegerType.create(32);
  }

  protected void setSignedLongLongType()
  {
    signedLongLongType = scale.clef.type.SignedIntegerType.create(64);
  }

  protected void setUnsignedLongLongType()
  {
    unsignedLongLongType = scale.clef.type.UnsignedIntegerType.create(64);
  }

  protected void setSizetType()
  {
    sizetType = scale.clef.type.UnsignedIntegerType.create(32);
  }

  protected void setVoidStarType()
  {
    voidStarType = scale.clef.type.PointerType.create(scale.clef.type.VoidType.type);
  }

  protected void setFloatType()
  {
    floatType = scale.clef.type.FloatType.create(32);
  }

  protected void setDoubleType()
  {
    doubleType = scale.clef.type.FloatType.create(64);
  }

  protected void setLongDoubleType()
  {
    longDoubleType = scale.clef.type.FloatType.create(64);
  }

  protected void setVaListType()
  {
    throw new scale.common.NotImplementedError("setVaListType");
  }

  public String getGenericArchitectureName()
  {
    return "Xyz";
  }

  public String getArchitectureName()
  {
    return "Xyz";
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

    // Determine what additional Xyz features are available.

    int len   = extension.length();
    int index = 0;
    while (index < len) {
      int sl = extension.indexOf('/', index);
      if (sl < 0)
        sl = len;
      String opt = extension.substring(index, sl);
      index = sl + 1;
    }

    return "scale.backend.xyz.XyzGenerator";
  }

  public int alignData(int dataSize)
  {
    throw new scale.common.NotImplementedError("alignData");
  }

  public int addressableMemoryUnits(int bitSize)
  {
    return ((bitSize + 7) / 8);
  }

  public boolean simpleInteger(long value)
  {
    int val = (int) value;
    return (val == value);
  }

  public int executionCostEstimate(long value)
  {
    throw new scale.common.NotImplementedError("executionCostEstimate");
  }

  public int executionCostEstimate(double value)
  {
    throw new scale.common.NotImplementedError("executionCostEstimate");
  }

  public int maxBitFieldSize()
  {
    return 64;
  }

  public String getAsmFileExtension()
  {
    return ".s";
  }
  
  public void addCPPFlags(Vector<String> v)
  {
    v.addElement("__Xyz=1");
  }

  public final int generalAlignment()
  {
    return 8;
  }

  public final int stackAlignment(Type type)
  {
    return 8;
  }

  public boolean littleEndian()
  {
    return false;
  }

  public String getAssemblerCommand(int backendFeatures)
  {
    StringBuffer buf = new StringBuffer("as ");
    if ((backendFeatures & Generator.DEBUG) != 0)
      buf.append("-g ");
    return buf.toString();
  }

  public boolean keepTypeInRegister(Type type, boolean temporary)
  {
    throw new scale.common.NotImplementedError("keepTypeInRegister");
  }

  public scale.backend.ICEstimator getInstructionCountEstimator()
  {
    return new scale.backend.ICEstimator(currentMachine);
  }

  public byte[] getFunctionalUnitDescriptions()
  {
    return functionalUnits;
  }

  private static final byte[] functionalUnits = {1, 4, 4, 2, 1};
}
