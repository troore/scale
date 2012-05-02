package scale.alias.shapirohorowitz;

import scale.common.*;
import scale.alias.*;
import scale.alias.steensgaard.*;

/**
 * A class which implements extension to Bjarne Steensgaard's alias analysis algorithm.
 * <p>
 * $Id: ShapiroHorowitz.java,v 1.19 2005-06-15 04:17:05 asmith Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
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
 * <li>types can point off to multiple types - upto k of them (assuming k categories)
 * </ul>
 */
public class ShapiroHorowitz extends AliasAnalysis
{
  /**
   * Indicate if the analysis operates on the entire program or just
   * one procedure.
   */
  private boolean interprocedural;

  /**
   * Stores number of categories that the algorithm is being parameterized for
   */
  private int numberCategories;

  /**
   * Create an instance of the Shapiro-Horwitz alias analysis algorithm.
   */
  public ShapiroHorowitz(int numberCategories)
  {
    this(true, numberCategories);
  }

  /**
   * Create an instance of the Shapiro-Horwitz alias analysis algorithm and 
   * indicate if it operates on the whole program.
   * @param interprocedural set to true if the analysis should be interprocedural
   */
  public ShapiroHorowitz(boolean interprocedural, int numberCategories)
  {
    super();
    this.interprocedural  = interprocedural;
    this.numberCategories = numberCategories;
  }
  
  /**
   * Return false because Shapiro-Horwitz's algorithm is not flow sensitive.
   * @return false
   **/
  public boolean isFlowSensitive()
  {
    return false;
  }

  /**
   * Return false because Shapiro-Horwitz's algorithm is not context sensitive.
   * @return false
   */
  public boolean isContextSensitive()
  {
    return false;
  }

  /**
   * Return true if the analysis is interprocedural.  We can parameterize
   * Shapiro-Horwitz's algorithm to work on a single procedure or the 
   * entire program.
   */
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
  public AliasVar addVariable(scale.clef.decl.Declaration decl)
  {
    TypeVarCat var  = new TypeVarCat(decl, numberCategories);
    int        id   = var.getECR().getsetID();
    int        ac   = (id % numberCategories);

    var.setCategory(ac);

    return var;
  }

  /**
   * Add a variable into the analyzer and indicate the variable is
   * in the same alias group as another variable.
   * information from the given alias variable.
   * @return the alias variable that represents the varaible.
   */
  public AliasVar addVariable(scale.clef.decl.Declaration decl, AliasVar v) 
  {
    AliasVar var = new TypeVarCat(decl, (TypeVarCat) v);
    return var;
  }

  /**
   * Inference rules for simple assignment (<tt>x = y</tt>).
   *
   * @param lhs the left hand side of the assignment.
   * @param rhs the right hand side of the assignment.
   */
  public void simpleAssign(AliasVar lhs, AliasVar rhs)
  {
    if (trace)
      System.out.println("shapiro: simple assign - " + lhs + " := " + rhs);

    if (rhs == null)
      return;

    LocationTypeCat type1 = (LocationTypeCat) ((TypeVarCat) lhs).getECR().getType();
    LocationTypeCat type2 = (LocationTypeCat) ((TypeVarCat) rhs).getECR().getType();

    for (int i= 0; i < numberCategories; i++) {
      ECR tau1 = type1.getLocation(i);
      ECR tau2 = type2.getLocation(i);
      if (!tau1.equivalent(tau2))
        tau1.cjoin(tau2);

      ECR lamda1 = type1.getFunction(i);
      ECR lamda2 = type2.getFunction(i);
      if (!lamda1.equivalent(lamda2))
        lamda1.cjoin(lamda2);
    }  
  }

  /**
   * Inference rule for the assignment of an address (<tt>x = &ampy</tt>).
   *
   * @param lhs the left hand side of the assignment.
   * @param addr the right hand side of the assignment (the address).
   */
  public void addrAssign(AliasVar lhs, AliasVar addr)
  {
    if (trace)
      System.out.println("shapiro: addr assign - " + lhs + " := " + addr);

    LocationTypeCat type1       = (LocationTypeCat) ((TypeVarCat) lhs).getECR().getType();
    ECR             tau2        = ((TypeVarCat) addr).getECR();
    int             categoryNum = ((TypeVarCat) addr).getCategory();

    ECR tau1 = type1.getLocation(categoryNum);
    
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
    if (trace)
      System.out.println("shapiro: ptr assign - " + lhs + " := " + ptr);

    LocationTypeCat type1 = (LocationTypeCat) ((TypeVarCat) lhs).getECR().getType();
    LocationTypeCat type2 = (LocationTypeCat) ((TypeVarCat) ptr).getECR().getType();

    for (int i = 0; i < numberCategories; i++) {
      ECR tau2 = type2.getLocation(i);
    
      if (tau2.getType() == AliasType.BOT) {
        tau2.setType(new LocationTypeCat(type1.getLocations(),type1.getFunctions()));
      } else {
        LocationTypeCat type3 = (LocationTypeCat) tau2.getType();
        for (int j = 0; j < numberCategories; j++) {
          ECR tau1   = type1.getLocation(j);
          ECR lamda1 = type1.getFunction(j);

          ECR tau3   = type3.getLocation(j);
          ECR lamda3 = type3.getFunction(j);

          if (!tau1.equivalent(tau3))
            tau1.cjoin(tau3);
          if (!lamda1.equivalent(lamda3))
            lamda1.cjoin(lamda3);
        }
      }
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
      System.out.println("shapiro: op assign - " + lhs);

    LocationTypeCat type1 = (LocationTypeCat) ((TypeVarCat) lhs).getECR().getType();

    int l = opnds.size();
    for (int i = 0; i < l; i++) {

      TypeVar tv = (TypeVar) opnds.elementAt(i);

      if (tv == null)
        continue;

      if (trace)
        System.out.println("\top = " + tv);

      ECR opnd = tv.getECR();
      LocationTypeCat typei = (LocationTypeCat) opnd.getType();

      for (int j = 0; j < numberCategories; j++) {
        ECR tau1   = type1.getLocation(j);
        ECR lamda1 = type1.getFunction(j);

        ECR taui   = typei.getLocation(j);
        ECR lamdai = typei.getFunction(j);

        if (!tau1.equivalent(taui))
          tau1.cjoin(taui);
        if (!lamda1.equivalent(lamdai))
          lamda1.cjoin(lamdai);
      }
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
      System.out.println("shapiro: heap assign - " + lhs);

    LocationTypeCat type1 = (LocationTypeCat) ((TypeVarCat) lhs).getECR().getType();
    for (int i = 0; i < numberCategories; i++) {
      ECR tau = type1.getLocation(i);
      if (tau.getType() == AliasType.BOT)
        tau.setType(new LocationTypeCat(numberCategories));
    }
  }

  /**
   * Inference rule for assigning to a pointer (<tt>*x = y</tt>).
   *
   * @param ptr the pointer representing the left hand side of the assignment.
   * @param rhs the right hand side of the assignment.
   */
  public void assignPtr(AliasVar ptr, AliasVar rhs)
  {
    if (trace)
      System.out.println("shapiro: assign ptr - " + ptr + " := " + rhs);

    LocationTypeCat type1 = (LocationTypeCat) ((TypeVarCat) ptr).getECR().getType();
    LocationTypeCat type2 = (LocationTypeCat) ((TypeVarCat) rhs).getECR().getType();

    for (int i = 0; i < numberCategories; i++) {
      ECR tau1 = type1.getLocation(i);
        
      if (tau1.getType() == AliasType.BOT) {
        tau1.setType(new LocationTypeCat(type2.getLocations(),type2.getFunctions()));
      } else {
        LocationTypeCat type3 = (LocationTypeCat) tau1.getType();
        for (int j = 0; j < numberCategories; j++) {
                
          ECR tau2   = type2.getLocation(j);
          ECR lamda2 = type2.getFunction(j);
                
          ECR tau3   = type3.getLocation(j);
          ECR lamda3 = type3.getFunction(j);

          if (!tau2.equivalent(tau3))
            tau3.cjoin(tau2);
          if (!lamda2.equivalent(lamda3))
            lamda3.cjoin(lamda2);
        }
      }
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
      System.out.println("shapiro: func def - " + func + " retval := " + retval);

    LocationTypeCat type = (LocationTypeCat) ((TypeVarCat) func).getECR().getType();

    for (int i = 0; i < numberCategories; i++) {
      ECR lamda = type.getFunction(i);
      if (lamda.getType() == AliasType.BOT) { // Create a new type with the params and retval alias variables.
        FunctionTypeCat fType = new FunctionTypeCat(numberCategories);
        LocationTypeCat typea;
        Vector<ECR>     lv;
        Vector<ECR>     fv;
        int    l  = params.size();
        for (int j = 0; j < l; j++) {
          typea = (LocationTypeCat) params.elementAt(j).getECR().getType();
          lv = typea.getLocations();
          fv = typea.getFunctions();
                 
          fType.addArgument(new ValueTypeCat(lv,fv));      
        }
        typea = (LocationTypeCat) ((TypeVarCat) retval).getECR().getType();
        lv = typea.getLocations();
        fv = typea.getFunctions();
            
        fType.setRetval(new ValueTypeCat(lv,fv));
        lamda.setType(fType);
      } else {
        ECR tau1;
        ECR tau2;
        ECR lamda1;
        ECR lamda2;

        // first, process the arguments.

        FunctionTypeCat f  = (FunctionTypeCat) lamda.getType();
        int             ll = f.numArguments();
        int             pl = params.size();

        assert (ll == pl) : "No. of parameters should be the same";

        for (int ii = 0; ii < ll; ii++) {
          LocationTypeCat type1 = (LocationTypeCat) f.getArgument(ii);
          LocationTypeCat type2 = (LocationTypeCat) params.elementAt(ii).getECR().getType();
          for (int j = 0; j < numberCategories; j++) {
            tau1 = type1.getLocation(j);
            tau2 = type2.getLocation(j);
            if (!tau1.equivalent(tau2))
              tau1.join(tau2);

            lamda1 = type1.getFunction(j);
            lamda2 = type2.getFunction(j);
            if (!lamda1.equivalent(lamda2))
              lamda1.join(lamda2);
          }
        }

        // then, process the return value
        ValueTypeCat alpha  = ((FunctionTypeCat) lamda.getType()).getRetval();
        ECR          alphar = ((TypeVarCat) retval).getECR();
        for (int j = 0; j < numberCategories; j++) {
          tau1 = alpha.getLocation(j);
          tau2 = ((ValueTypeCat) alphar.getType()).getLocation(j);
          if (!tau1.equivalent(tau2))
            tau1.join(tau2);
      
          lamda1 = alpha.getFunction(j);
          lamda2 = ((ValueTypeCat) alphar.getType()).getFunction(j);
          if (!lamda1.equivalent(lamda2))
            lamda1.join(lamda2);
        }
      }
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
      System.out.println("shapiro: func call - " + lhs + " := " + func);

    LocationTypeCat type        = (LocationTypeCat) ((TypeVarCat) func).getECR().getType();
    int             categoryNum = ((TypeVarCat) func).getCategory();

    ECR lamda = type.getFunction(categoryNum);

    if (lamda.getType() == AliasType.BOT) {
      FunctionTypeCat lam = new FunctionTypeCat(numberCategories);
      // create the arguments ECRs
      int l = args.size();
      for (int j = 0; j < l; j++)
        lam.addArgument(new ValueTypeCat(numberCategories));
      // create the return value ECR
      lam.setRetval(new ValueTypeCat(numberCategories));
      lamda.setType(lam);
    }

    // Now, process the arguments.

    ECR          tau1;
    ECR          tau2;
    ECR          lamda1;
    ECR          lamda2;

    FunctionTypeCat ftc   = (FunctionTypeCat) lamda.getType();
    int             ll    = ftc.numArguments();
    int             la    = args.size();
    ValueTypeCat    type1 = null;

    assert (ll > 0) : "All functions need at least one alias var to represent formals.";

    for (int i = 0; i < la; i++) {

      // If there are more actual parms than formal parms then this
      // function must have a variable number of args (or no formals are
      // declared, but the function has actuals).  We only use one
      // alias var to represent all the actuals.

      if (i < ll)
        type1 = (ValueTypeCat) ftc.getArgument(i);

      LocationTypeCat type2 = (LocationTypeCat) args.elementAt(i).getECR().getType();

      for (int j = 0; j < numberCategories; j++) {
        tau1 = type1.getLocation(j);
        tau2 = type2.getLocation(j);
        if (!tau1.equivalent(tau2))
          tau1.cjoin(tau2);

        lamda1 = type1.getFunction(j);
        lamda2 = type2.getFunction(j);
        if (!lamda1.equivalent(lamda2))
          lamda1.cjoin(lamda2);
      }
    }

    // then, process the return value - if there is no return value then
    // just skip this part
    if (lhs != null) {
      ValueTypeCat alpha  = ((FunctionTypeCat) lamda.getType()).getRetval();
      ECR          alphar = ((TypeVarCat) lhs).getECR();
    
      for (int j = 0; j < numberCategories; j++) {
        tau1 = alpha.getLocation(j);
        tau2 = ((LocationTypeCat) alphar.getType()).getLocation(j);
        if (!tau1.equivalent(tau2))
          tau2.cjoin(tau1);
      
        lamda1 = alpha.getFunction(j);
        lamda2 = ((LocationTypeCat) alphar.getType()).getFunction(j);
        if (!lamda1.equivalent(lamda2))
          lamda2.cjoin(lamda1);
      }
    }
  }

  /**
   * Remove all the un-needed stuff.
   */
  public void cleanup()
  {
  }
}
