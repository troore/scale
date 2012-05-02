package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file source file
 * attribute structure and to read that class file source file
 * attribute structure.
 * <p>
 * $Id: SourceFileAttribute.java,v 1.9 2007-10-04 19:58:17 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class SourceFileAttribute extends AttributeInfo
{
  private int sourceFileIndex;

  public SourceFileAttribute(int nameIndex, int sourceFileIndex)
  {
    super(nameIndex);
    this.sourceFileIndex = sourceFileIndex;
  }

  public int getSourceFileIndex()
  {
    return sourceFileIndex;
  }

  public static SourceFileAttribute read(ClassFile       cf,
                                         DataInputStream reader,
                                         int             nameIndex) throws java.io.IOException
  {
    int attributeLength = reader.readInt();
    int sourceFileIndex = reader.readUnsignedShort();
    return new SourceFileAttribute(nameIndex, sourceFileIndex);
  }
}
