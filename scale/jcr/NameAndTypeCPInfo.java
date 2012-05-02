package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file name and
 * type constant pool entry and to read that class file name and type
 * constant pool entry.
 * <p>
 * $Id: NameAndTypeCPInfo.java,v 1.9 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class NameAndTypeCPInfo extends CPInfo
{
  private int nameIndex;
  private int descriptorIndex;

  public NameAndTypeCPInfo(int nameIndex, int descriptorIndex)
  {
    super(CPInfo.CONSTANT_NameAndType);
    this.nameIndex = nameIndex;
    this.descriptorIndex = descriptorIndex;
  }

  public int getNameIndex()
  {
    return nameIndex;
  }

  public int getDescriptorIndex()
  {
    return descriptorIndex;
  }

  public static CPInfo read(ClassFile       cf,
                            DataInputStream reader) throws java.io.IOException
  {
    int nameIndex       = reader.readUnsignedShort();
    int descriptorIndex = reader.readUnsignedShort();
    return new NameAndTypeCPInfo(nameIndex, descriptorIndex);
  }
}
