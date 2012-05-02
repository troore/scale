package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file local
 * variable entry and to read that class file local variable entry.
 * <p>
 * $Id: LocalVariableEntry.java,v 1.9 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LocalVariableEntry
{
  public int startPc;
  public int length;
  public int nameIndex;
  public int descriptorIndex;
  public int index;

  public LocalVariableEntry(int startPc,
                            int length,
                            int nameIndex,
                            int descriptorIndex,
                            int index)
  {
    this.startPc         = startPc;
    this.length          = length;
    this.nameIndex       = nameIndex;
    this.descriptorIndex = descriptorIndex;
    this.index           = index;
  }

  public static LocalVariableEntry read(ClassFile       cf,
                                        DataInputStream reader) throws java.io.IOException
  {
    int startPc         = reader.readUnsignedShort();
    int length          = reader.readUnsignedShort();
    int nameIndex       = reader.readUnsignedShort();
    int descriptorIndex = reader.readUnsignedShort();
    int index           = reader.readUnsignedShort();
    return new LocalVariableEntry(startPc, length, nameIndex, descriptorIndex, index);
  }
}
