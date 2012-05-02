package scale.annot;

/**
 * This class is used to specify who created an Anotation.
 * <p>
 * $Id: Creator.java,v 1.16 2007-08-13 12:32:02 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Anotations carry an indication of what tool created them.
 * This is for debugging and error checking.  We use a typed
 * object class hierarchy to represent the creator.  
 * Some sample sub-classes for the Creator class are as follows:
 *   <UL>
 *   <LI>CreatorSource
 *   <LI>CreatorAnnotationTool
 *   <LI>CreatorClef2score
 *   <LI>CreatorClefBuilder
 *   <LI>CreatorAliasAnalysis
 *   </UL>
 * We use a class hierarchy because it gives us more flexibility.
 * Note - by class hierarchy we mean a hierarchy for which all
 * instances of a specific sub-class are considered equal.  Hence, an
 * instance can be refered to by its super-class.
 * @see scale.annot.Annotation
*/
public abstract class Creator
{
  /**
   * The name of the creator of the annotation.
   */
  private String name;

  /**
   * @param name is the name of creator of the annotation.
   */
  public Creator(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(Creator ");
    buf.append(name);
    buf.append(")");
    return buf.toString();
  }
}
