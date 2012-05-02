package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file other
 * attributes structure and to read that class file other attributes
 * structure.
 * <p>
 * $Id: OtherAttribute.java,v 1.9 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class OtherAttribute extends AttributeInfo
{
  private byte[] info;

  public OtherAttribute(int nameIndex, byte[] info)
  {
    super(nameIndex);

    this.info = info;
  }

  public byte[] getInfo()
  {
    return info;
  }

  public static OtherAttribute read(ClassFile       cf,
                                    DataInputStream reader,
                                    int             nameIndex) throws java.io.IOException
  {
    int attributeLength = reader.readInt();

    byte[] info = new byte[attributeLength];
    reader.read(info);

    return new OtherAttribute(nameIndex, info);
  }
}
