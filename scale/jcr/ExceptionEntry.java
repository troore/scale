package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file exception
 * entry structure and to read that class file exception entry
 * structure.
 * <p>
 * $Id: ExceptionEntry.java,v 1.9 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ExceptionEntry
{
  public int startPc;
  public int endPc;
  public int handlerPc;
  public int catchType;

  public ExceptionEntry(int startPc, int endPc, int handlerPc, int catchType)
  {
    this.startPc   = startPc;
    this.endPc     = endPc;
    this.handlerPc = handlerPc;
    this.catchType = catchType;
  }

  public static ExceptionEntry read(ClassFile       cf,
                                    DataInputStream reader) throws java.io.IOException
  {
    int startPc   = reader.readUnsignedShort();
    int endPc     = reader.readUnsignedShort();
    int handlerPc = reader.readUnsignedShort();
    int catchType = reader.readUnsignedShort();
    return new ExceptionEntry(startPc, endPc, handlerPc, catchType);
  }
}
