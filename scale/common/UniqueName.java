package scale.common;

/**
 * This class implements a unique name generator.
 * <p>
 * $Id: UniqueName.java,v 1.17 2007-01-04 16:58:39 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class UniqueName 
{

  /**
   * A counter for the unique name generation.
   */
  private int counter = 0;

  private String nameSeed = "_tmp";

  /**
   * @param seed the name prefix to be used for generated names.
   */
  public UniqueName(String seed)
  {
    nameSeed = seed;
    if (nameSeed == null)
      nameSeed = "_tmp";
  }

  /**
   * Return a unique name.
   * Generate a string of the form
   * <code>
   * pppnnnn
   * </code>
   * where <code>ppp</code> is the seed specified when the UniqueName
   * object was created and <code>nnn</code> is a unique integer.
   */
  public synchronized String genName()
  {
    return nameSeed + counter++;
  }

  /**
   * Return the next name that will be generated.  This is useful for
   * debugging.  To get a name for use in generated code use {@link
   * #genName genName}.
   */
  public String getNextName()
  {
    return nameSeed + counter;
  }
  /**
   * Return the next number that will be used in the next generated
   * name.  This is useful for debugging.  To get a name for use in
   * generated code use {@link #genName genName}.
   */
  public int getNextNumber()
  {
    return counter;
  }
}
