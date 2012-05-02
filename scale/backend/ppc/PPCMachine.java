package scale.backend.ppc;

import scale.common.*;
import scale.clef.type.*;
import scale.clef.decl.FieldDecl;

/**
 * This is the base class for all PPC specific information.
 * <p>
 * $Id: PPCMachine.java,v 1.31 2007-11-01 16:52:28 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PPCMachine extends Machine
{
  /**
   * Specifies the G4 32-bit PowerPC.
   */
  public static final int G4 = 0;
  /**
   * Specifies the G5 64-bit PowerPC.
   */
  public static final int G5 = 1;
  /**
   * Specifies the linux operating system.
   */
  public static final int LINUX = 0;
  /**
   * Specifies the mac os x operating system.
   */
  public static final int MACOSX = 1;

  private int    isa;
  private String isas = null;
  private int    os;
  private String oss = null;

  /**
   * Create a Machine instance for an PPC.
   */
  public PPCMachine()
  {
    super(0);

    scale.clef.type.PointerType.setMinBitSize(32);
  }

  /**
   * Return the name of the generic target architecture.
   */
  public String getGenericArchitectureName()
  {
    return "ppc";
  }

  /**
   * Return the name of the specific target architecture.
   */
  public String getArchitectureName()
  {
    return "ppc";
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
    isas = "g4";
    isa  = G4;
    oss  = "linux";
    os   = MACOSX;
        
    int    sl         = extension.indexOf('/');
    String extension2 = "";
        
    if (sl > 0) {
      extension2 = extension.substring(sl + 1);
      extension = extension.substring(0, sl);
    }

    if (extension.length() > 0) {
      isas = extension.toLowerCase();

      if ("g4".equals(isas))
        isa = G4;
      else if ("g5".equals(isas)) 
        isa = G5;
      else if ("macosx".equals(isas))
        os = MACOSX;
      else if ("linux".equals(isas))
        os = LINUX;
      else
        throw new java.lang.Exception("Unknown architecture extension - " + extension);
    }

    if (extension2.length() > 0) {
      isas = extension2.toLowerCase();

      if ("g4".equals(isas))
        isa = G4;
      else if ("g5".equals(isas)) 
        isa = G5;
      else if ("linux".equals(isas))
        os = LINUX;
      else if ("macosx".equals(isas))
        os = MACOSX;
      else
        throw new java.lang.Exception("Unknown architecture extension - " + extension2);
    }
        
    return "scale.backend.ppc.PPCGenerator";
  }

  /**
   * Return the specific instruction set architecture.
   */
  public final int getISA()
  {
    return isa;
  }

  /**
   * Return the specific operating system.
   */
  public final int getOS()
  {
    return os;
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
    int val = (int) value;
    if ((long) val == value)
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
    return 5;
  }

  /**
   * Add the flags necessary for the C preprocessor.
   */
  public void addCPPFlags(Vector<String> v)
  {
    v.addElement("__ppc__=1");
    v.addElement("__PPC=1");
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
    return type.alignment(this);
  }

  /**
   * Return true if the machine is little-endian.
   */
  public boolean littleEndian()
  {
    return false;
  }

  /**
   * Should a value of this type be allocated to a register?
   * @param type is the type
   * @param temporary is true if the duration will be short
   *
   * super.keepTypeInRegister() takes care of what we need to do. We only
   * need to ensure that aggregates (structs, unions, etc.) are not put in
   * registers.
   */
  public boolean keepTypeInRegister(Type type, boolean temporary)
  {
    return type.isAggregateType() ? false :
      super.keepTypeInRegister(type, temporary);
  }

  protected void setup()
  {
    super.setup();

    if (LINUX == os) {
      // va_list has this structure.
      //
      // overflow_arg_area is initially the address at which the first arg
      // passed on the stack, if any, was stored.
      //
      // reg_save_area is the start of where r3:r10 were stored.
      // reg_save_area must be a doubleword aligned.
      //
      // If f1:f8 have been stored (because CR bit 6 was 1),
      // reg_save_area+4*8 must be the start of where f1:f8 were stored
      //
      // typedef struct {
      //   char gpr; /* index into the array of 8 GPRs
      //              * stored in the register save area gpr=0 corresponds to
      //              * r3, gpr=1 to r4, etc.
      //              */
      //   char fpr; /* index into the array of 8 FPRs
      //              * stored in the register save area fpr=0 corresponds to
      //              * f1, fpr=1 to f2, etc.
      //              */
      //   char *overflow_arg_area;
      //             /* location on stack that holds the next overflow
      //              * argument
      //              */
      //   char *reg_save_area;
      //             /* where r3:r10 and f1:f8 (if saved) are stored
      //              */
      // } va_list;
      Type   gpr               = CharacterType.create(CharacterType.cAnsi);
      Type   fpr               = CharacterType.create(CharacterType.cAnsi);
      Type   overflow_arg_area = PointerType.create(
                                 CharacterType.create(CharacterType.cAnsi));
      Type   reg_save_area     = PointerType.create(
                                 CharacterType.create(CharacterType.cAnsi));
      Vector<FieldDecl> fields = new Vector<FieldDecl>(4);
      fields.addElement(new FieldDecl("gpr", gpr, 0));
      fields.addElement(new FieldDecl("fpr", fpr, 4));
      fields.addElement(new FieldDecl("overflow_arg_area", overflow_arg_area, 8));
      fields.addElement(new FieldDecl("reg_save_area", reg_save_area, 12));
      vaListType = RecordType.create(fields);
    }
  }
}
