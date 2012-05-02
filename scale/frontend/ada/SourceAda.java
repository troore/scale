package scale.frontend.ada;

import scale.frontend.*;
import scale.clef.type.*;
import scale.clef.decl.ParameterMode;
import scale.clef.decl.FormalDecl;

/**
 * A class which defines source language characteristics for Ada.
 * <p>
 * $Id: SourceAda.java,v 1.1 2006-12-05 21:02:08 burrill Exp $
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The compiler uses these methods to generate valid code for Ada
 * programs.
 */
public class SourceAda extends SourceLanguage
{
  /**
   * Ada arrays are laid out in row major order.
   *
   * @return cRowMajor
   */
  public boolean arrayOrdering()
  {
    return SourceLanguage.cRowMajor;
  }

  /**
   * Ada arrays start at origin 0.
   * @return 0.
   */
  public int arrayIndexOrigin()
  {
    return 0;
  }

  /**
   * Ada parameters are passed by value.
   * @param t the type of the parameter is ignored
   * @return the parameter passing mode
   * @see scale.clef.decl.FormalDecl
   */
  public ParameterMode parameterPassing(Type t)
  {
    return ParameterMode.VALUE;
  }

  /**
   * Ada names are mangled.
   * @return true 
   */
  public boolean nameMangle()
  {
    return true;
  }

  /**
   * Ada defines the Main function to be the routine "main".
   * @return true
   */
  public boolean mainFunction()
  { 
    return true;
  }

  /**
   * @return the LanguageId associated with this source language
   */
  public String getLanguageId()
  {
    return "Cxx";
  }
}
