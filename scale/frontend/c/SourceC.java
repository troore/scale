package scale.frontend.c;

import scale.clef.type.*;
import scale.clef.decl.ParameterMode;
import scale.clef.decl.FormalDecl;
import scale.frontend.*;

/**
 * A class which defines source language characteristics for C.
 * <p>
 * $Id: SourceC.java,v 1.1 2006-12-05 21:02:08 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The compiler uses these methods to generate valid code for C
 * programs.
 */
public class SourceC extends SourceLanguage
{

  /**
   * C arrays are laid out in row major order.
   *
   * @return cRowMajor
   */
  public boolean arrayOrdering()
  {
    return SourceLanguage.cRowMajor;
  }

  /**
   * C arrays start at index 0.
   * @return 0
   */
  public int arrayIndexOrigin()
  {
    return 0;
  }

  /**
   * C parameters are passed by value.
   * @param t the parameter's type is ignored.
   * @return pass by value.
   */
  public ParameterMode parameterPassing(Type t)
  {
    return ParameterMode.VALUE;
  }

  /**
   * C does not mangle names.
   * @return false
   */
  public boolean nameMangle()
  {
    return false;
  }

  /**
   * C defines the Main function to be the routine "main".
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
    return "C";
  }
}
