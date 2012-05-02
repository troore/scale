package scale.jcr;

/**
 * This class is abstract super class for all of the Java class file
 * Info structures.
 * <p>
 * $Id: Info.java,v 1.9 2007-01-04 17:01:16 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class Info
{
  private int             accessFlags;
  private int             nameIndex;
  private int             descriptorIndex;
  private AttributeInfo[] attributes;

  public Info(int             accessFlags,
              int             nameIndex,
              int             descriptorIndex,
              AttributeInfo[] attributes)
  {
    this.accessFlags = accessFlags;
    this.nameIndex = nameIndex;
    this.descriptorIndex = descriptorIndex;
    this.attributes = attributes;
  }

  public int getAccessFlags()
  {
    return accessFlags;
  }

  public int getNameIndex()
  {
    return nameIndex;
  }

  public int getDescriptorIndex()
  {
    return descriptorIndex;
  }

  public AttributeInfo[] getAttributes()
  {
    return attributes.clone();
  }

  public AttributeInfo getAttribute(int i)
  {
    return attributes[i];
  }
}
