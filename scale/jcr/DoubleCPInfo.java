package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file double value
 * constant pool structure and to read that class file double value
 * constant pool structure.
 * <p>
 * $Id: DoubleCPInfo.java,v 1.9 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class DoubleCPInfo extends CPInfo
{
  private double value;

  public DoubleCPInfo(double value)
  {
    super(CPInfo.CONSTANT_Double);
    this.value = value;
  }

  public double getValue()
  {
    return value;
  }

  public static CPInfo read(ClassFile       cf,
                            DataInputStream reader) throws java.io.IOException
  {
    double value = reader.readDouble();
    return new DoubleCPInfo(value);
  }
}
