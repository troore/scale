package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file
 * AttributeInfo structure and to read that class file AttributeInfo
 * structure.
 * <p>
 * $Id: AttributeInfo.java,v 1.10 2007-10-04 19:58:14 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class AttributeInfo
{
  private int nameIndex;

  public AttributeInfo(int nameIndex)
  {
    this.nameIndex = nameIndex;
  }

  /**
   * Return the index of the name of the attribute in the constant pool.
   */
  public int getNameIndex()
  {
    return nameIndex;
  }

  /**
   * Read in the AttributeInfo structure.
   */
  public static AttributeInfo read(ClassFile       cf,
                                   DataInputStream reader) throws java.io.IOException
  {
    int        nameIndex = reader.readUnsignedShort();
    Utf8CPInfo name      = (Utf8CPInfo) cf.getCP(nameIndex);
    String     n         = name.getString();

    if (n.equals("SourceFile")) {
      return SourceFileAttribute.read(cf, reader, nameIndex);
    } else if (n.equals("ConstantValue")) {
      return ConstantValueAttribute.read(cf, reader, nameIndex);
    } else if (n.equals("Code")) {
      return CodeAttribute.read(cf, reader, nameIndex);
    } else if (n.equals("Exceptions")) {
      return ExceptionsAttribute.read(cf, reader, nameIndex);
    } else if (n.equals("LineNumberTable")) {
      return LineNumberTableAttribute.read(cf, reader, nameIndex);
    } else if (n.equals("LocalVariableTable")) {
      return LocalVariableTableAttribute.read(cf, reader, nameIndex);
    } else {
      return OtherAttribute.read(cf, reader, nameIndex);
    }
  }
}
