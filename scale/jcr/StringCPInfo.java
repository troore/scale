package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file string
 * constant pool entry and to read that class file string constant
 * pool entry.
 * <p>
 * $Id: StringCPInfo.java,v 1.9 2007-10-04 19:58:17 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class StringCPInfo extends CPInfo
{
  private int stringIndex;

  public StringCPInfo(int stringIndex)
  {
    super(CPInfo.CONSTANT_String);
    this.stringIndex = stringIndex;
  }

  public static CPInfo read(ClassFile       cf,
                            DataInputStream reader) throws java.io.IOException
  {
    int stringIndex = reader.readUnsignedShort();
    return new StringCPInfo(stringIndex);
  }

  public int getStringIndex()
  {
    return stringIndex;
  }

}
