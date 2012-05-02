package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file exception
 * attribute structure and to read that class file exception attribute
 * structure.
 * <p>
 * $Id: ExceptionsAttribute.java,v 1.10 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ExceptionsAttribute extends AttributeInfo
{
  private int[] exceptionIndexTable;

  public ExceptionsAttribute(int nameIndex, int[] exceptionIndexTable)
  {
    super(nameIndex);
    this.exceptionIndexTable = exceptionIndexTable;
  }

  public int numExceptions()
  {
    return exceptionIndexTable.length;
  }

  public int getExceptionIndex(int i)
  {
    return exceptionIndexTable[i];
  }

  public static ExceptionsAttribute read(ClassFile       cf,
                                         DataInputStream reader,
                                         int             nameIndex) throws java.io.IOException
  {
    int attributeLength       = reader.readInt();
    int numberOfExceptions    = reader.readUnsignedShort();
    int[] exceptionIndexTable = new int[numberOfExceptions];
    for (int i = 0; i < numberOfExceptions; i++)
      exceptionIndexTable[i] = reader.readUnsignedShort();

    return new ExceptionsAttribute(nameIndex, exceptionIndexTable);
  }
}
