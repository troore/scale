package scale.frontend;

import scale.clef.decl.ParameterMode;
import scale.clef.type.*;

/**
 * An abstract class for supporting multiple source languages.
 * <p>
 * $Id: SourceLanguage.java,v 1.1 2006-12-05 21:02:07 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class contains methods which define certain characteristics
 * about the source language.  The compile needs to know about
 * specific source language details to generate valid code.
 */
public abstract class SourceLanguage
{
  /**
   * Array ordering is row major.
   */
  public static final boolean cRowMajor = true;
  /**
   * Array ordering is column major
   */
  public static final boolean cColumnMajor = false;

  /**
   * Implemenations of the generation interface are required to handle
   * both case sensitive and insensitive identifiers.  The default is
   * case sensitive.  We could have required the front end to
   * homogenize case for case insensitive languages; however, this
   * information may prove vital to debuggers.
   * <p>
   * The language is case-sensitive.
   */
  public static final boolean cSensitive = true;
  /**
   * The language is case-insensitive.
   */
  public static final boolean cInsensitive = false;

  /**
   * Are identifiers case sensitive.
   */
  private boolean identifierCase = cSensitive;

  /**
   * These attributes specify how dynamic memory is managed by the
   * source language.
   * <p>
   * C++ is an example of a user-managed memory management language.
   */
  public static final boolean cUserManaged = true;
  /**
   * Java is an example of a garbage-collected memory management
   * language.
   */
  public static final boolean cGarbageCollected = false;

  /**
   * The type of memory model used.
   */
  private boolean memManage = cUserManaged;
  /**
   * Does the order of record fields matter?
   */
  private boolean recordFieldOrderMatters = true;
  /**
   * Does the order of class fields matter?
   */
  private boolean classFieldOrderMatters  = false;
  /**
   * Does the order of methods matter?
   */
  private boolean methodOrderMatters      = false;

  /**
   * Specifies the way that arrays are laid out.  Arrays are either
   * laid out in <i>row major</i> order or <i>column major</i> order.<br>
   * Row major means that the last subscript varies most rapily.<br>
   * Column major means that the first subscript varies most rapidl.
   *
   * @return true if the language uses row major order.
   */
  public abstract boolean arrayOrdering();

  /**
   * Return the array index origin for the source language. (For
   * example, C starts at 0 while in Fortran they start at index 1.
   */
  public abstract int arrayIndexOrigin();

  /**
   * Specifies the parameter passing mode used by default. For
   * example, pass by value.  The parameter passing mode may depend
   * upon the type of the actual parameter.
   * @param t the type of the actual parameter
   * @return the passing mode for the parameter
   * @see scale.clef.decl.FormalDecl
   */
  public abstract ParameterMode parameterPassing(Type t);

  /**
   * Returns true if names need to be mangled (Function Name
   * Encoding).
   */
  public abstract boolean nameMangle();

  /**
   * Returns true if the source language defines "main" (e.g, C).
   */
  public abstract boolean mainFunction();

  /**
   * Return the string associated with this source language.
   */
  public abstract String getLanguageId();

  /**
   * Return true if identifiers are case sensitive.
   */
  public boolean isCaseSensitive()
  {
    return identifierCase;
  }

  /**
   * Specify if the language is case sensitive.
   */
  public void setIdentifierCase(boolean cs)
  {
    identifierCase = cs;
  }

  /**
   * Return true if the lanuage relies on user-written memory
   * management.  C++ is an example of a user-managed memory
   * management language.
   */
  public boolean isMemUserManaged()
  {
    return memManage;
  }

  /**
   * Specify the memory management used by the language.
   * @param mm is true if the language relies on user-written memory
   * management.  C++ is an example of a user-managed memory
   * management language.
   */
  public void setMemoryManagement(boolean mm)
  {
    memManage = mm;
  }

  /**
   * Specify if the order of record fields in memory matters.
   */
  public void setRecordFieldOrderRule(boolean orderMatters)
  {
     recordFieldOrderMatters = orderMatters;
  }

  /**
   * Return true if the order of record fields in memory matters.
   * Java is an example of a language where the order does not matter.
   */
  public boolean recordFieldOrdermatters()
  {
    return recordFieldOrderMatters;
  }

  /**
   * Specify if the order of class fields in memory matters.
   */
  public void setClassFieldOrderRule(boolean orderMatters) 
  {
    classFieldOrderMatters = orderMatters;
  }

  /**
   * Return true if the order of class fields in memory matters.  Java
   * is an example of a language where the order does not matter.
   */
  public boolean classFieldOrderMatters()
  {
    return classFieldOrderMatters;
  }

  /**
   * Specify if the order of class methods in the virtual table
   * matters.
   */
  public void setMethodsRule(boolean methodsMatter) 
  {
    methodOrderMatters = methodsMatter;
  }

  /**
   * Return true if the order of class methods in the virtual table
   * matters.  Java is an example of a language where the order does
   * not matter.
   */
  public boolean mathodOrderMatters()
  {
    return methodOrderMatters;
  }

  /**
   * Return true if the source langauge is Fortran.
   */
  public boolean isFortran()
  {
    return false;
  }
}
