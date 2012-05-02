package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file line number
 * table and to read that class file line number table.
 * <p>
 * $Id: LineNumberTableAttribute.java,v 1.11 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LineNumberTableAttribute extends AttributeInfo
{
  private LineNumberEntry[] lineNumberTable;

  public LineNumberTableAttribute(int nameIndex, LineNumberEntry[] lineNumberTable)
  {
    super(nameIndex);
    this.lineNumberTable = lineNumberTable;
  }

  public int getLineNumberTableLength()
  {
    return lineNumberTable.length;
  }

  public LineNumberEntry[] getLineNumberTable()
  {
    return lineNumberTable;
  }

  public LineNumberEntry getLineNumberEntry(int i)
  {
    return lineNumberTable[i];
  }

  public static LineNumberTableAttribute read(ClassFile       cf,
                                              DataInputStream reader,
                                              int             nameIndex) throws java.io.IOException
  {
    int               attributeLength = reader.readInt();
    int               length          = reader.readUnsignedShort();
    LineNumberEntry[] lineNumberTable = new LineNumberEntry[length];
    for (int i = 0; i < length; i++)
      lineNumberTable[i] = LineNumberEntry.read(cf, reader);

    return new LineNumberTableAttribute(nameIndex, lineNumberTable);
  }
}
