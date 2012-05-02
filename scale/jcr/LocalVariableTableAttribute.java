package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file local
 * variable table and to read that class file local variable table.
 * <p>
 * $Id: LocalVariableTableAttribute.java,v 1.10 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LocalVariableTableAttribute extends AttributeInfo
{
  private LocalVariableEntry[] localVariableTable;

  public LocalVariableTableAttribute(int nameIndex, LocalVariableEntry[] localVariableTable)
  {
    super(nameIndex);
    this.localVariableTable = localVariableTable;
  }

  public int getLocalVariableTablelength()
  {
    return localVariableTable.length;
  }

  public LocalVariableEntry getLocalVariableEntry(int i)
  {
    return localVariableTable[i];
  }

  public static LocalVariableTableAttribute read(ClassFile       cf,
                                                 DataInputStream reader,
                                                 int             nameIndex) throws java.io.IOException
  {
    int                  attributeLength    = reader.readInt();
    int                  length             = reader.readUnsignedShort();
    LocalVariableEntry[] localVariableTable = new LocalVariableEntry[length];
    for (int i = 0; i < length; i++)
      localVariableTable[i] = LocalVariableEntry.read(cf, reader);

    return new LocalVariableTableAttribute(nameIndex, localVariableTable);
  }
}
