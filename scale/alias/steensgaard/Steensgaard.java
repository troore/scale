package scale.alias.steensgaard;

import scale.common.*;
import scale.alias.*;
import scale.clef.decl.Declaration;

/**
 * A class which implements Bjarne Steensgaard's alias analysis algorithm.
 * <p>
 * $Id: Steensgaard.java,v 1.41 2005-05-09 17:10:34 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The algorithm performs interprocedural flow-insensitive points-to
 * analysis.  The important features of the algorithm are:
 * <ul>
 * <li>It runs in almost linear time.
 * <li>It is based on type inference methods.
 * </ul>
 */
public class Steensgaard extends AliasAnalysis
{
  /**
   * Indicate if the analysis operates on the entire program or just
   * one procedure.
   **/
  private boolean interprocedural;

  /**
   * Create an instance of the Steensgaard alias analysis algorithm.
   **/
  public Steensgaard()
  {
    this(true);
  }

  /**
   * Create an instance of the Steensgaard alias analysis algorithm and 
   * indicate if it operates on the whole program.
   * @param ip set to true if the analysis should be interprocedural
   **/
  public Steensgaard(boolean ip)
  {
    super();
    interprocedural = ip;
  }
  
  /**
   * Return false because Steensgaard's algorithm is not flow sensitive.
   * @return false
   **/
  public boolean isFlowSensitive()
  {
    return false;
  }

  /**
   * Return false because Steensgaard's algorithm is not context sensitive.
   * @return false
   **/
  public boolean isContextSensitive()
  {
    return false;
  }

  /**
   * Return true if the analysis is interprocedural.  We can parameterize
   * Steensgaard's algorithm to work on a single procedure or the 
   * entire program.
   **/
  public boolean isInterProcedural()
  {
    return interprocedural;
  }

  /**
   * Add a variable that needs to be processed by the algorithm.  We
   * create a unique non-standard type for the variable and add it
   * to the fast union/find data structure.
   *
   * @param decl is the name of the variable.
   * @return our alias representation of the varaible.
   */
  public AliasVar addVariable(Declaration decl)
  {
    return new TypeVar(decl);
  }

  /**
   * Add a variable into the analyzer and indicate the variable is
   * in the same alias group as another variable.
   * information from the given alias variable.
   * @return the alias variable that represents the varaible.
   **/
  public AliasVar addVariable(Declaration decl, AliasVar v) 
  {
    return new TypeVar(decl, (TypeVar) v);
  }

  /**
   * Inference rules for simple assignment (<tt>x = y</tt>).
   *
   * @param lhs the left hand side of the assignment.
   * @param rhs the right hand side of the assignment.
   */
  public void simpleAssign(AliasVar lhs, AliasVar rhs)
  {
    if (rhs == null)
      return;

    if (trace)
      System.out.println("steen: simple assign - " + lhs + " := " + rhs);

    LocationType type1 = (LocationType) ((TypeVar) lhs).getECR().getType();
    LocationType type2 = (LocationType) ((TypeVar) rhs).getECR().getType();

    ECR tau1 = type1.getLocation();
    ECR tau2 = type2.getLocation();
    if (!tau1.equivalent(tau2))
      tau1.cjoin(tau2);

    ECR lamda1 = type1.getFunction();
    ECR lamda2 = type2.getFunction();
    if (!lamda1.equivalent(lamda2))
      lamda1.cjoin(lamda2);
  }

  /**
   * Inference rule for the assignment of an address (<tt>x = &amp;y</tt>).
   *
   * @param lhs the left hand side of the assignment.
   * @param addr the right hand side of the assignment (the address).
   */
  public void addrAssign(AliasVar lhs, AliasVar addr)
  {
    if (addr == null)
      return;

    if (trace)
      System.out.println("steen: addr assign - " + lhs + " := &" + addr);

    LocationType type1 = (LocationType) ((TypeVar) lhs).getECR().getType();
    ECR          tau1  = type1.getLocation();
    ECR          tau2  = ((TypeVar) addr).getECR();

    if (!tau1.equivalent(tau2))
      tau1.join(tau2);
  }

  /**
   * Inference rule for a pointer assignment (<tt>x = *y</tt).
   *
   * @param lhs the left hand side of the assignment.
   * @param ptr the right hand side of the assignment (the pointer).
   */
  public void ptrAssign(AliasVar lhs, AliasVar ptr)
  {
    if (ptr == null)
      return;

    if (trace)
      System.out.println("steen: ptr assign - " + lhs + " := *" + ptr);

    LocationType type1 = (LocationType) ((TypeVar) lhs).getECR().getType();
    LocationType type2 = (LocationType) ((TypeVar) ptr).getECR().getType();

    ECR tau1   = type1.getLocation();
    ECR tau2   = type2.getLocation();
    ECR lamda1 = type1.getFunction();
    
    if (tau2.getType() == AliasType.BOT) {
      tau2.setType(type1);
    } else {
      LocationType type3 = (LocationType) tau2.getType();

      ECR tau3 = type3.getLocation();
      if (!tau1.equivalent(tau3))
        tau1.cjoin(tau3);

      ECR lamda3 = type3.getFunction();
      if (!lamda1.equivalent(lamda3))
        lamda1.cjoin(lamda3);
    }
  }

  /**
   * Inference rule for an operation <code>(x = op(y1,&hellip;,yN))</code>.
   * 
   * @param lhs the left hand side of the assignment.
   * @param opnds the list of arguments of the operation.
   */
  public void opAssign(AliasVar lhs, Vector<AliasVar> opnds)
  {
    if (trace)
      System.out.println("steen: op assign - " + lhs);

    ECR          ecr   = lhs.getECR();
    LocationType type1 = (LocationType) ecr.getType();

    ECR tau1   = type1.getLocation();
    ECR lamda1 = type1.getFunction();

    int l = opnds.size();
    for (int i = 0; i < l; i++) {
      TypeVar tv = (TypeVar) opnds.elementAt(i);

      if (tv == null)
        continue;

      if (trace)
        System.out.println("\top = " + tv);

      ECR          opnd   = tv.getECR();
      LocationType typei  = (LocationType) opnd.getType();
      ECR          taui   = typei.getLocation();
      ECR          lamdai = typei.getFunction();

      if (!tau1.equivalent(taui))
        tau1.cjoin(taui);
      if (!lamda1.equivalent(lamdai))
        lamda1.cjoin(lamdai);
    }
  }

  /**
   * Inference rule for assigning dynamically allocated memory.
   *
   * @param lhs the left hand side of the assignment.
   */
  public void heapAssign(AliasVar lhs) 
  {
    if (trace)
      System.out.println("steen: heap assign - " + lhs);

    LocationType type1 = (LocationType) ((TypeVar) lhs).getECR().getType();
    ECR          tau   = type1.getLocation();

    if (tau.getType() == AliasType.BOT)
      tau.setType(new LocationType());
  }

  /**
   * Inference rule for assigning to a pointer (<tt>*x = y</tt>).
   *
   * @param ptr the pointer representing the left hand side of the assignment.
   * @param rhs the right hand side of the assignment.
   */
  public void assignPtr(AliasVar ptr, AliasVar rhs)
  {
    if (rhs == null)
      return;

    if (trace)
      System.out.println("steen: assign ptr - *" + ptr + " := " + rhs);

    LocationType type1 = (LocationType) ((TypeVar) ptr).getECR().getType();
    LocationType type2 = (LocationType) ((TypeVar) rhs).getECR().getType();

    ECR tau1   = type1.getLocation();
    ECR tau2   = type2.getLocation();
    ECR lamda2 = type2.getFunction();

    if (tau1.getType() == AliasType.BOT) {
      tau1.setType(type2);
    } else {
      LocationType type3  = (LocationType) tau1.getType();
      ECR          tau3   = type3.getLocation();
      ECR          lamda3 = type3.getFunction();
      if (!tau2.equivalent(tau3))
        tau3.cjoin(tau2);
      if (!lamda2.equivalent(lamda3))
        lamda3.cjoin(lamda2);
    }
  }

  /**
   * Inference rule for a function definition.
   *
   * @param func the alias variable representing the function.
   * @param params the alias variables representing the list of parameters.
   * @param retval the alias variables representing the return value.
   */
  public void functionDef(AliasVar func, Vector<AliasVar> params, AliasVar retval)
  {
    if (trace)
      System.out.println("steen: func def - " + func + " retval := " + retval);

    LocationType type  = (LocationType) func.getECR().getType();
    ECR          lamda = type.getFunction();

    if (lamda.getType() == AliasType.BOT) { // Create a new type with the params and retval alias variables.
      FunctionType      fType = new FunctionType();
      Vector<ValueType> args  = fType.getArguments();
      int               l     = params.size();

      for (int i = 0; i < l; i++) {
         LocationType typea = (LocationType) params.elementAt(i).getECR().getType();
        args.addElement(new ValueType(typea.getLocation(), typea.getFunction()));
      }

      LocationType typea = (LocationType) retval.getECR().getType();
      fType.setRetval(new ValueType(typea.getLocation(), typea.getFunction()));
      lamda.setType(fType);
    } else {

      // First, process the arguments.

      Vector<ValueType> args = ((FunctionType) lamda.getType()).getArguments();
      int    la   = args.size();
      int    lp   = params.size();

      assert (la == lp) : "No. of parameters should be the same.";

      for (int i = 0; i < lp; i++) {
        LocationType type1 = (LocationType) params.elementAt(i).getECR().getType();
        LocationType type2 = (LocationType) args.elementAt(i).getLocation().getType();

        ECR tau1 = type1.getLocation();
        ECR tau2 = type2.getLocation();
        if (!tau1.equivalent(tau2))
          tau1.join(tau2);

        ECR lamda1 = type1.getFunction();
        ECR lamda2 = type2.getFunction();
        if (!lamda1.equivalent(lamda2))
          lamda1.join(lamda2);
      }

      // Then, process the return value.

      ValueType alpha  = ((FunctionType) lamda.getType()).getRetval();
      ECR       alphar = ((TypeVar) retval).getECR();

      ECR tau1 = alpha.getLocation();
      ECR tau2 = ((ValueType) alphar.getType()).getLocation();
      if (!tau1.equivalent(tau2))
        tau1.join(tau2);
      
      ECR lamda1 = alpha.getFunction();
      ECR lamda2 = ((ValueType) alphar.getType()).getFunction();
      if (!lamda1.equivalent(lamda2))
        lamda1.join(lamda2);
    }

  }

  /**
   * Inference rule for a function call assignment.
   *
   * @param lhs the alias variables representing the value returned
   * by the function call (is null if there function returns nothing)
   * @param func the alias variables representing the function.
   * @param args alias variables representing the arguments.
   */
  public void functionCall(AliasVar lhs, AliasVar func, Vector<AliasVar> args) 
  {
    if (trace)
      System.out.println("steen: func call - " + lhs + " := " + func);

    LocationType type  = (LocationType) func.getECR().getType();
    ECR          lamda = type.getFunction();

    if (lamda.getType() == AliasType.BOT) {
      FunctionType lam = new FunctionType();
      int l = args.size();
      if (l == 0)
        l = 1;
      lam.addNewArguments(l); // Create the arguments ECRs.
      lam.setRetval(new ValueType());   // Create the return value ECR.
      lamda.setType(lam);
    }

    // Now, process the arguments

    Vector<ValueType> largs = ((FunctionType) lamda.getType()).getArguments();
    int               ll    = largs.size();
    int               la    = args.size();
    ValueType         type1 = null;

    assert ((ll > 0) || (la <= 0)) : "All functions need at least one alias var to represent formals.";

    for (int i = 0; i < la; i++) {
      // If there are more actual parms than formal parms then this
      // function must have a variable number of args (or no formals are
      // declared, but the function has actuals).  We only use one
      // alias var to represent all the actuals.

      if (i < ll)
        type1 = largs.elementAt(i);

      LocationType type2 = (LocationType) ((TypeVar) args.elementAt(i)).getECR().getType();

      ECR tau1 = type1.getLocation();
      ECR tau2 = type2.getLocation();
      if (!tau1.equivalent(tau2))
        tau1.cjoin(tau2);

      ECR lamda1 = type1.getFunction();
      ECR lamda2 = type2.getFunction();
      if (!lamda1.equivalent(lamda2))
        lamda1.cjoin(lamda2);
    }

    // Then, process the return value - if there is no return value then just skip this part.

    if (lhs != null) {
      ValueType alpha  = ((FunctionType) lamda.getType()).getRetval();
      ECR       alphar = ((TypeVar) lhs).getECR();

      ECR tau1   = alpha.getLocation();
      ECR tau2   = ((LocationType) alphar.getType()).getLocation();
      if (!tau1.equivalent(tau2))
        tau2.cjoin(tau1);
      

      ECR lamda1 = alpha.getFunction();
      ECR lamda2 = ((LocationType) alphar.getType()).getFunction();
      if (!lamda1.equivalent(lamda2))
        lamda2.cjoin(lamda1);
    }
  }
}
