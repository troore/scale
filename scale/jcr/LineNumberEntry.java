package scale.jcr;

import java.io.*;

/**
 * This class is used to both represent a Java class file line number
 * entry and to read that class file line number entry.
 * <p>
 * $Id: LineNumberEntry.java,v 1.9 2007-10-04 19:58:16 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LineNumberEntry
{
  public int startPc;
  public int lineNumber;

  public LineNumberEntry(int startPc, int lineNumber)
  {
    this.startPc    = startPc;
    this.lineNumber = lineNumber;
  }

  public static LineNumberEntry read(ClassFile       cf,
                                     DataInputStream reader) throws java.io.IOException
  {
    int startPc    = reader.readUnsignedShort();
    int lineNumber = reader.readUnsignedShort();
    return new LineNumberEntry(startPc, lineNumber);
  }
}
