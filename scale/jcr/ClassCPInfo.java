package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file class
 * constant pool entry and to read that class file class constant pool
 * entry.
 * <p>
 * $Id: ClassCPInfo.java,v 1.9 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ClassCPInfo extends CPInfo
{
  private int nameIndex;

  public ClassCPInfo(int nameIndex)
  {
    super(CPInfo.CONSTANT_Class);
    this.nameIndex = nameIndex;
  }

  public int getNameIndex()
  {
    return nameIndex;
  }

  public static CPInfo read(ClassFile       cf,
                            DataInputStream reader) throws java.io.IOException
  {
    int nameIndex = reader.readUnsignedShort();
    return new ClassCPInfo(nameIndex);
  }
}
