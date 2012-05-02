package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file constant
 * value attribute structure and to read that class file constant
 * value attribute structure.
 * <p>
 * $Id: ConstantValueAttribute.java,v 1.9 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ConstantValueAttribute extends AttributeInfo
{
  private int constantValueIndex;

  public ConstantValueAttribute(int nameIndex, int constantValueIndex)
  {
    super(nameIndex);
    this.constantValueIndex = constantValueIndex;
  }

  public int getConstantValueIndex()
  {
    return constantValueIndex;
  }

  public static ConstantValueAttribute read(ClassFile       cf,
                                            DataInputStream reader,
                                            int             nameIndex) throws java.io.IOException
  {
    int attributeLength    = reader.readInt();
    int constantValueIndex = reader.readUnsignedShort();
    return new ConstantValueAttribute(nameIndex, constantValueIndex);
  }
}
