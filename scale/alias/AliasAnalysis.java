package scale.alias;

import scale.common.*;

/**
 * A class for computing aliases among variables.
 * <p>
 * $Id: AliasAnalysis.java,v 1.18 2005-02-07 21:27:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class AliasAnalysis
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  protected     boolean trace;

  public AliasAnalysis()
  {
    this.trace = classTrace || Debug.debug(3);
  }

  /**
   * Return true if the analysis is flow sensitive.  That is, does 
   * the analysis care about statement order.
   * @return true if the analysis is flow sensitive.
   **/
  public abstract boolean isFlowSensitive();
  /**
   * Return true if the analysis is context sensitive.  That is,
   * does the analysis care about function call context.
   * @return true if the analysis is context sensitive.
   **/
  public abstract boolean isContextSensitive();
  /**
   * Return true if the analysis is interprocedural.  An analysis that
   * is not interprocedural must make very simplifying assumptions at
   * procedure calls.  Note - by definition, a flow and context
   * sensitive analysis is interprocedural.
   **/
  public abstract boolean isInterProcedural();

  /**
   * Add a variable into the the analyzer.
   * @param decl is the name of the variable.
   * @return the alias variable that represents the varaible.
   */
  public abstract AliasVar addVariable(scale.clef.decl.Declaration decl);

  /**
   * Add a variable into the analyzer and use some of the information
   * from an existing alias variable.
   * @return the alias variable that represents the varaible.
   **/
  public abstract AliasVar addVariable(scale.clef.decl.Declaration decl, AliasVar v);

  /**
   * Compute aliases for assignment statement (<tt>x = y</tt>).
   *
   * @param lhs the left hand side of the assignment (lvalue).
   * @param rhs the righ hand side of the assignment (rvalue).
   */
  public abstract void simpleAssign(AliasVar lhs, AliasVar rhs);

  /**
   * Compute aliases for the assignment of an address (<tt>x = &ampy</tt>).
   *
   * @param lhs is the left hand side of the assignment (lvalue).
   * @param addr is the right hand side of the assignment (the address).
   */
  public abstract void addrAssign(AliasVar lhs, AliasVar addr);

  /**
   * Compute aliases for a pointer assignment (<tt>x = *y</tt>).
   *
   * @param lhs is the left hand side of the assignment.
   * @param ptr is the right hand side of the assignment (the pointer).
   */
  public abstract void ptrAssign(AliasVar lhs, AliasVar ptr);

  /**
   * Compute alias for an operation <code>(x = op(y1,&hellip;,yN))</code>.
   * 
   * @param lhs is the left hand side of the assignment.
   * @param opnds is the list of arguments of the operation.
   */
  public abstract void opAssign(AliasVar lhs, Vector<AliasVar> opnds);

  /**
   * Compute aliases for assigning dynamically allocated memory.
   *
   * @param lhs is the left hand side of the assignment.
   */
  public abstract void heapAssign(AliasVar lhs);

  /**
   * Compute aliases for assigning to a pointer (<tt>*x = y</tt>).
   *
   * @param ptr the pointer representing the left hand side of the assignment.
   * @param rhs the right hand side of the assignment.
   */
  public abstract void assignPtr(AliasVar ptr, AliasVar rhs);

  /**
   * Compute aliases for the formal parameters and return value
   * of a function definition.
   *
   * @param func the alias variable representing the function.
   * @param params the alias variables representing the list of parameters.
   * @param retval the alias variables representing the return value.
   */
  public abstract void functionDef(AliasVar func, Vector<AliasVar> params, AliasVar retval);

  /**
   * Compute aliases caused by a function call.
   *
   * @param lhs the alias variables representing the value returned
   * by the function call.
   * @param func the alias variables representing the function.
   * @param args alias variables representing the arguments.
   */
  public abstract void functionCall(AliasVar lhs, AliasVar func, Vector<AliasVar> args);
}
