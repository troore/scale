package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file method
 * reference constant pool entry and to read that class file method
 * reference constant pool entry.
 * <p>
 * $Id: MethodRefCPInfo.java,v 1.9 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class MethodRefCPInfo extends RefCPInfo
{
  public MethodRefCPInfo(int classIndex, int nameAndTypeIndex)
  {
    super(CPInfo.CONSTANT_Methodref, classIndex, nameAndTypeIndex);
  }

  public static CPInfo read(ClassFile       cf,
                            DataInputStream reader) throws java.io.IOException
  {
    int classIndex       = reader.readUnsignedShort();
    int nameAndTypeIndex = reader.readUnsignedShort();
    return new MethodRefCPInfo(classIndex, nameAndTypeIndex);
  }
}
