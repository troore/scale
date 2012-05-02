package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file constant
 * pool entry and to read that class file constant pool entry.
 * <p>
 * $Id: CPInfo.java,v 1.9 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class CPInfo
{
  /**
   * The constant pool entry tags.
   */
  public static final int CONSTANT_Utf8               = 1;
  public static final int CONSTANT_Integer            = 3;
  public static final int CONSTANT_Float              = 4;
  public static final int CONSTANT_Long               = 5;
  public static final int CONSTANT_Double             = 6;
  public static final int CONSTANT_Class              = 7;
  public static final int CONSTANT_String             = 8;
  public static final int CONSTANT_Fieldref           = 9;
  public static final int CONSTANT_Methodref          = 10;
  public static final int CONSTANT_InterfaceMethodref = 11;
  public static final int CONSTANT_NameAndType        = 12;

  private int tag;

  public CPInfo(int tag)
  {
    this.tag = tag;
  }

  public int getTag()
  {
    return tag;
  }

  public static CPInfo read(ClassFile       cf,
                            DataInputStream reader) throws java.io.IOException,
                                                           scale.common.InvalidException
  {
    int rtag = reader.readUnsignedByte();

    switch (rtag) {
      case CONSTANT_Utf8:               return Utf8CPInfo.read(cf, reader);
      case CONSTANT_Integer:            return IntCPInfo.read(cf, reader);
      case CONSTANT_Float:              return FloatCPInfo.read(cf, reader);
      case CONSTANT_Long:               return LongCPInfo.read(cf, reader);
      case CONSTANT_Double:             return DoubleCPInfo.read(cf, reader);
      case CONSTANT_Class:              return ClassCPInfo.read(cf, reader);
      case CONSTANT_String:             return StringCPInfo.read(cf, reader);
      case CONSTANT_Fieldref:           return FieldRefCPInfo.read(cf, reader);
      case CONSTANT_Methodref:          return MethodRefCPInfo.read(cf, reader);
      case CONSTANT_InterfaceMethodref: return InterfaceMethodRefCPInfo.read(cf, reader);
      case CONSTANT_NameAndType:        return NameAndTypeCPInfo.read(cf, reader);
    }
    throw new scale.common.InvalidException("Unknown constant pool entry tag " + rtag);
  }
}
