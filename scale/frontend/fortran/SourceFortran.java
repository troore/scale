package scale.frontend.fortran;

import scale.clef.type.*;
import scale.clef.decl.ParameterMode;
import scale.clef.decl.FormalDecl;
import scale.frontend.*;

/**
 * A class which defines source language characteristics for Fortran77.
 * <p>
 * $Id: SourceFortran.java,v 1.1 2006-12-05 21:02:10 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The compiler uses these methods to generate valid code for Fortran
 * programs.
 */
public class SourceFortran extends SourceLanguage
{
  /**
   * Fortran arrays are laid out in column major order.
   * @return cColumnMajor
   */
  public boolean arrayOrdering()
  {
    return SourceLanguage.cColumnMajor;
  }

  /**
   * Fortran arrays start at index value 1.
   * @return 1
   */
  public int arrayIndexOrigin() 
  { 
    return 1;
  }

  /** 
   * By default, Fortran passes arrays by reference except for arrays
   * which are passed by address pass by value.
   * @param t the type of the actual parameter.
   * @return the parameter passing mode
   * @see scale.clef.decl.FormalDecl
   */
  public ParameterMode parameterPassing(Type t)
  {
    if (t.isPointerType() || t.isArrayType()) // As Fortran does not have pointers, this can only be an array reference.
      return ParameterMode.VALUE;

    return ParameterMode.REFERENCE;
  }

  /**
   * Fortran does not need to mangle names.
   * @return false
   */
  public boolean nameMangle()
  {
    return false;
  }

  /**
   * Fortran does not define the Main function to be the routine "main".
   * @return false
   */
  public boolean mainFunction()
  { 
    return false;
  }

  /**
   * Return the LanguageId associated with this source language.
   */
  public String getLanguageId()
  {
    return "Fortran77";
  }

  /**
   * Return true if the source langauge is Fortran.
   */
  public final boolean isFortran()
  {
    return true;
  }
}
