package scale.jcr;

/**
 * This class is the abstract class for all reference constant pool entries.
 * <p>
 * $Id: RefCPInfo.java,v 1.8 2005-02-07 21:28:30 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class RefCPInfo extends CPInfo
{
  private int classIndex;
  private int nameAndTypeIndex;

  public RefCPInfo(int tag, int classIndex, int nameAndTypeIndex)
  {
    super(tag);
    this.classIndex = classIndex;
    this.nameAndTypeIndex = nameAndTypeIndex;
  }

  public int getClassIndex()
  {
    return classIndex;
  }

  public int getNameAndTypeIndex()
  {
    return nameAndTypeIndex;
  }
}
