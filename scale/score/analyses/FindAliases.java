package scale.score.analyses;

import java.util.Iterator;

import scale.common.*;
import scale.alias.*;
import scale.annot.*;
import scale.callGraph.Suite;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.RoutineDecl;
import scale.clef.type.*;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.pred.*;

/**
 * This class visits nodes in order to compute aliases.
 * <p>
 * $Id: FindAliases.java,v 1.84 2007-10-04 19:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * We visit expression nodes and compute aliases when for each
 * assignment operation.  We expect that
 * this predicate is used by the <tt>TraceChords</tt> predicate
 * (which is responsible for idenitifing all expression nodes in
 * a graph).  The user may also provide a different technique for
 * traversing the graph in order to find expression nodes.
 * <p>
 * We traversing the tree we sometimes need to create new temporary alias 
 * variables.  For example, this happens when we compute some value
 * that is not assigned to a temporary or real variable.  We add the
 * temporary alias variable as an annotation (AliasAnnote).  When we
 * are done using the temporary, we need to explictily remove it.  
 * At some point, we need to improve/fix the annotation code or use
 * a different technique for saving the temporary alias variables.
 * <p>
 * The code to find the aliases depends on the structure of the Scribble
 * tree.  Any major changes to the structure must be reflected in this code.
 * <p>
 *
 * @see scale.score.chords.ExprChord
 */
public class FindAliases extends TraceChords 
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * Set true to trace operation.
   */
  protected boolean trace = false;
  /** 
   * The alias analysis algorithm. 
   */
  private AliasAnalysis analyzer;

  /**
   * Pointer to Aliases object to add alias variables
   * to aliasVars vector
  */
   private Aliases aliasesObject;
 
  /**
   * Create a unique name for alias temporaries
   */
  private UniqueName unique = new UniqueName("__t");
  /**
   * The suite containing the procedures in the program.
   */
  private Suite suite;
  /** 
   * Used for annotations. 
   */
  private static final CreatorSource creator = new CreatorSource("FindAliases");

  /**
   * Create an object to find aliases in a scribble/score graph.
   * @param analyzer the alias analysis algorithm.
   * @param suite the suite containing the procedures in the program.
   */
  public FindAliases(AliasAnalysis analyzer, Suite suite, Aliases aliasesObject)
  {
    this.analyzer      = analyzer;
    this.suite         = suite;
    this.aliasesObject = aliasesObject;
  }

  /**
   * Find the aliases for the specified routine.
   */
  public void findAliases(RoutineDecl cn, Stack<Chord> wl)
  {
    assert setTrace(cn.getName());

    Scribble fn = cn.getScribbleCFG();
    assert (fn != null) : "No Scribble CFG for " + cn.getRoutineName();
     
    Chord start = fn.getBegin(); // get the first statement in the function

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) { // find the aliases
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);
      c.visit(this);
    }
  }

  private boolean setTrace(String name)
  {
    trace = Debug.trace(name, classTrace, 3);
    return true;
  }

  /**
   * Check if the expression is involved in an assignment statement.
   * If so, we return the lhs of the assignment. If not, return null.
   *
   * @param expr the expression node.
   * @return the LHS of the assignment if <i>expr</i> is assigned.  Otherwise,
   * return null.
   */
  private Expr isAssigned(Expr expr)
  {
    if (expr.isDefined()) // is it an lvalue.
      return expr;

    Note n = expr.getOutDataEdge();
    if (n instanceof ExprChord) // check that this node is involved in an assignment
      return ((ExprChord) n).getLValue();

    return null;
  }

  /**
   * Return true if the lhs of a store is a simple expression.  A
   * simple expression is a basic variable reference.  A complex
   * expression is an operation such as indirection, structure field
   * reference, or array reference.
   *
   * @param the lhs of a Store expression.
   * @return true if the lhs is a simple expression.
   */
  private boolean isSimpleExpr(Expr lhs)
  {
    return (lhs instanceof LoadDeclAddressExpr);
  }

  /** 
   * Get the alias information from a Declaration.  The
   * alias information is placed on Declaration nodes as annotations.
   * The Declaration nodes are attached to Scribble load nodes.
   *
   * @param d the Declaration node that contains the alias annotation
   * @return the alias variable associated with the declaration.
   */
  private AliasVar getAliasVar(Declaration d)
  {
    AliasAnnote annote = (AliasAnnote) d.getAnnotation(AliasAnnote.annotationKey());
    assert (annote != null) : "Variable without alias variable " + d;
    return annote.getAliasVar();
  }

  /**
   * Get the alias variable associated with a Scribble operator.  Most Scribble
   * operators do not have alias variables so this routine returns a dummy alias variable.
   * Typically, the alias variable information is attached to the declaration
   * node associated with the load operations.  However, we sometimes need to
   * create alias variables to hold alias information that is not directly 
   * assigned to a user variable (e.g., <tt>**x</tt>).
   * <p>
   * For <tt>LoadDeclValueExpr</tt> and <tt>LoadDeclAddressExpr</tt> the information
   * is located in the "extra" information.  For <tt>LoadValueIndirectExpr</tt>,
   * the information is located in the operand.  
   *
   * @param expr the expression representing the operator.
   * @return the alias variable associated with a Scribble operator.
   */
  private AliasVar getAliasVar(Expr expr)
  {
    // Check if the expression node has the alias info - this means that
    // we've created a 'temporary' variable to hold the alias info.
    // We also remove the 'temporary' alias variable from the expression to
    // reclaim memory - it's a create once, use once value.

    AliasAnnote annote = expr.getAliasAnnote();
    if (annote != null)
      return annote.getAliasVar();

    return analyzer.addVariable(new VariableDecl("_gav_dummy", VoidType.type));
  }

  /**
   * Create an alias variable to hold the alias name.
   */
  private VariableDecl createNewTemp(String msg, Type type, Note n, Expr lhs)
  {
    VariableDecl tdecl = new VariableDecl(unique.genName(), type);

    if (Debug.debug(2)) {
      System.out.print(msg);
      System.out.print(": New temporary variable: ");
      System.out.println(tdecl);
      if (n != null) {
        System.out.print("  Node = ");
        System.out.println(n);
      }
      if (lhs != null) {
        System.out.print("  lhs = ");
        System.out.println(lhs);
      }
    }
    return tdecl;
  }

  /**
   * When performing intraprocedural analysis, we need to be
   * conservative around function calls with the actual 
   * parameters.  
   * - An actual can take the address of a global
   * - The actuals may be potential aliases of each other
   */
  private void intraFunctionCall(Vector<AliasVar> argAliasVars)
  {
    // Conservatively assume an actual parameter can potentially
    // point to any global
    int                   l  = argAliasVars.size();
    Iterator<Declaration> it = suite.topLevelDefDecls();
    while (it.hasNext()) {
      Declaration d = it.next();
      if (d.isVariableDecl()) {
        AliasVar av = getAliasVar(d);
        for (int i = 0; i < l; i++) {
          AliasVar argav = argAliasVars.elementAt(i);
          analyzer.addrAssign(argav, av);
        }
      }
    }

    // Conservatively assume assignments among the actual parameters
    // (e.g., in foo(&a, &b) a and b may be aliases)
    for (int i = 0; i < l; i++) {
      AliasVar argi = argAliasVars.elementAt(i);
      for (int j = 0; j < l; j++) {
        if (i == j) {
          continue;
        }
        AliasVar argj = argAliasVars.elementAt(j);
        analyzer.simpleAssign(argi, argj);
      }
    }
  }

  /**
   * Compute aliases at a function call node (<tt>FunctionCallOp</tt>.
   * This method handles the case when the function returns void and
   * there is no need for a store.
   */
  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    if (trace)
      System.out.println("FindAliases: " + e);

    Expr lhs = isAssigned(e);

    // Compute aliases here if the function call isn't assigned.
    // If the lhs is not a simple variable reference we need to create an alias var.

    if ((lhs == null) || !isSimpleExpr(lhs)) {
      Expr             routine      = e.getFunction();    // Get the expression representing the routine name.
      Vector<AliasVar> argAliasVars = new Vector<AliasVar>();       // Create a vector for the arguments.
      int              l            = e.numArguments();

      for (int i = 0; i < l; i++) {
        Expr     arg = e.getArgument(i);
        AliasVar av  = getAliasVar(arg);
     
        if (av != null)
          argAliasVars.addElement(av);
      }

      AliasVar tempVar = null;
      if (lhs instanceof LoadDeclValueExpr) {

        // Create a temp. alias var. if the lhs is an indirection op.

        if (lhs instanceof LoadDeclValueExpr) {
          Declaration tdecl = createNewTemp("visitCallFunctionExpr",
                                            PointerType.create(lhs.getType()),
                                            e,
                                            lhs);
          tempVar = analyzer.addVariable(tdecl);

          aliasesObject.addAliasVarToVector(tempVar);
        }
      }

      // Compute aliases

      AliasVar fav = getAliasVar(routine);
      analyzer.functionCall(tempVar, fav, argAliasVars);

      // If the analysis is not interprocedural, then we need to be conservative
      // with global variables and the actual parameters

      if (!analyzer.isInterProcedural()) {
        intraFunctionCall(argAliasVars);
      }
    }
  }
    

  /**
   * Compute aliases for method calls.
   */
  public void visitCallMethodExpr(CallMethodExpr e)
  {
    // Are methods and functions really the same????
    visitCallFunctionExpr((CallFunctionExpr) (CallExpr) e);
  }

  /**
   * Compute aliases at a <tt>LoadDeclAddressExpr</tt> node. This method
   * handles the case when we have <tt>&expr</tt> and the value is not
   * directly stored anywhere.  By directly stored, we mean that the
   * LHS is a simple variable and does not involve any indirection.
   * We need to create an alias variable to hold the result.
   */
  public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e)
  {
    if (trace)
      System.out.println("FindAliases: " + e);

    Expr lhs = isAssigned(e);

    // If the expression isn't assigned or the lhs is not a simple
    // variable reference then we need to create an alias variable.

    if (!(e.getDecl().isRoutineDecl()) && ((lhs == null) || !isSimpleExpr(lhs))) {
      Declaration tdecl   = createNewTemp("visitLoadDeclAddressExpr", e.getType(), e, lhs);
      AliasVar    tempVar = analyzer.addVariable(tdecl);
      aliasesObject.addAliasVarToVector(tempVar);
      AliasVar    exprVar = getAliasVar(e);

      assert (exprVar != null) : "No alias variable for " + e.getDecl();

      analyzer.addrAssign(tempVar, exprVar);
      // Wait until after call to assign alias annotation (because of getAliasVar).
      AliasAnnote.create(e, creator, Support.systemTrue, tempVar);
    }
  }

  /**
   * Compute aliases at a <tt>LoadDeclValueExpr</tt> node.  Most of the
   * time this case is handled in <tt>visitStoreOp</tt>.  However,
   * we need to check for som special cases here.
   * <p>
   * We check for array loads that appear on the RHS.  In this case,
   * we treat the operation as the expression (<tt>&ampa[0]</tt>).
   * @see scale.score.expr.LoadDeclValueExpr
   */
  public void visitLoadDeclValueExpr(LoadDeclValueExpr e)
  {
    Type type = e.getCoreType();

    if (trace)
      System.out.println("FindAliases: " + e);


    if (type.isArrayType()) {
      Declaration tdecl   = createNewTemp("visitLoadDeclValueExpr",
                                          PointerType.create(type),
                                          e,
                                          null);
      AliasVar    tempVar = analyzer.addVariable(tdecl);
      aliasesObject.addAliasVarToVector(tempVar);

      analyzer.addrAssign(tempVar, getAliasVar(e));

      // Wait until after call to assign alias annotation (because of getAliasVar).

      AliasAnnote.create(e, creator, Support.systemTrue, tempVar);
    }
  }

  /**
   * Compute aliases at a <tt>LoadValueIndirectExpr</tt> node.  This
   * method handles the case when we have <tt>*expr</tt> and the value
   * is not directly stored anywhere.  By directly stored, we mean
   * that the LHS is a simple variable and does not involve any
   * indirection.  We need to create an alias variable to hold the
   * result.
   *
   * @see scale.score.expr.LoadValueIndirectExpr
   */
  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr e)
  {
    if (trace)
      System.out.println("FindAliases: " + e);
    
    Expr lhs = isAssigned(e);
    // If the load indirect is on the LHS then deal with that
    // in visitExprChord, otherwise, 
    // if the expression isn't assigned or the lhs is not a simple
    // variable reference then we need to create an alias variable.
    if ((lhs == null) || ((lhs != e) && !isSimpleExpr(lhs))) {
      Type        type    = e.getCoreType();
      Declaration tdecl   = createNewTemp("visitLoadValueIndirectExpr", type, e, lhs);
      AliasVar    tempVar = analyzer.addVariable(tdecl);
      aliasesObject.addAliasVarToVector(tempVar);

      if (type.isProcedureType())
        analyzer.simpleAssign(tempVar, getAliasVar(e));
      else
        analyzer.ptrAssign(tempVar, getAliasVar(e));

      // Wait until after call to assign alias annotation (because of getAliasVar).

      AliasAnnote.create(e, creator, Support.systemTrue, tempVar);
    }
  }
  
  /**
   * This is the main routine for handling aliases.  We also create alias vars.
   * in the other methods defined in this class when the expressions
   * do not fit in the following cases:
   * <ul>
   * <li> <tt>x = y</tt>
   * <li> <tt>x = *y</tt>
   * <li> <tt>x = &y</tt>
   * <li> <tt>*x = y</tt>
   * <li> <tt>x = op(y1,...,yN)</tt>
   * <li> <tt>x = malloc()</tt>
   * <li> <tt>x = func(p1,..,pN)</tt>
   * <li> <tt>r.f = y</tt> or <tt>x->f = y</tt>
   * <li> <tt>a[s] = y</tt> 
   * </ul>
   * The representation of these constructs in Scribble are different
   * so each case must be handled separately.
   *
   * @see scale.score.chords.ExprChord
   */
  public void visitExprChord(ExprChord c)
  {
    visitChord(c);

    Expr rvalue = c.getRValue();
    Expr lvalue = c.getLValue();

    if (lvalue == null)
      return;

    lvalue = lvalue.getLow(); // By default, work on the lowered version.
    rvalue = rvalue.getLow(); // By default, work on the lowered version.

    if (trace)
      System.out.println("FindAliases: " + c);
    
    AliasVar av = getAliasVar(rvalue);

    // Call the appropriate alias analysis routine which is based upon
    // the operands of the assignment.  We also need to get the alias
    // variable from the declaration node of the lhs and rhs. The
    // postition of the alias variable depends upon the operand type.

    AliasVar lhsAV = getAliasVar(lvalue);

    if (lvalue instanceof LoadDeclAddressExpr) {

      if (rvalue instanceof LoadDeclValueExpr) {
        // x = y
        if (av != null)
          analyzer.simpleAssign(lhsAV, av);
      } else if (rvalue instanceof LoadValueIndirectExpr) {
        // x = *y
        if (av != null)
          analyzer.ptrAssign(lhsAV, av);
      } else if (rvalue instanceof LoadDeclAddressExpr) {
        // x = &y
        if (av != null)
          analyzer.addrAssign(lhsAV, av);
      } else if (rvalue instanceof LoadFieldAddressExpr) {
        // x = & foo->a
        if (av != null)
          analyzer.addrAssign(lhsAV, av);
      } else if (rvalue instanceof LoadFieldValueExpr) {
        // x = foo->a
        if (av != null)
          analyzer.simpleAssign(lhsAV, av);
      } else if (rvalue instanceof CallExpr) {

        CallExpr ce = (CallExpr) rvalue;
        // get the expression representing the routine name
        Expr routine = ce.getFunction();

        // check if this is a heap allocation - for this we explicitly check
        // for malloc.  Otherwise, it is just a regular function call.

        if ((routine instanceof LoadDeclAddressExpr) &&
            ((LoadDeclAddressExpr) routine).getName().equalsIgnoreCase("malloc")) {
          // heap assignment
          analyzer.heapAssign(lhsAV);
        } else {
          // This is a regular function call - create a vector for the arguments.
          Vector<AliasVar> argAliasVars = new Vector<AliasVar>();
          int              l            = ce.numArguments();
          for (int i = 0; i < l; i++) {
            Expr     arg = ce.getArgument(i);
            AliasVar aav = getAliasVar(arg);
            if (aav != null) 
              argAliasVars.addElement(aav);
          }

          analyzer.functionCall(lhsAV, getAliasVar(routine), argAliasVars);
          if (!analyzer.isInterProcedural()) {
            intraFunctionCall(argAliasVars);
          }
        }
      } else { // x = op(y1, ..., yN)
        Vector<AliasVar> opAliasVars = new Vector<AliasVar>();
        int              l           = rvalue.numOperands();

        for (int i = 0; i < l; i++) {
          Expr op = rvalue.getOperand(i);
          AliasVar aav = getAliasVar(op);
          if (aav != null)
            opAliasVars.addElement(aav);
        }
        analyzer.opAssign(lhsAV, opAliasVars);
      }
    } else if (lvalue instanceof LoadDeclValueExpr) { // *x = y
      // Note - the rhs is either a simple variable or we have create
      // an alias variable for it.
      if (av != null)
        analyzer.assignPtr(lhsAV, av);
    } else if (lvalue instanceof LoadFieldAddressExpr) { // x.f = y (or x->f = y)
      // Note - we treat an assignment to a field as an assigment to the struct.
      if (av != null)
        analyzer.simpleAssign(lhsAV, av);
    } else if (lvalue instanceof LoadFieldValueExpr) { // *x.f = y (or *x->f = y)
      // Note - we treat an assignment to a field as an assigment to the struct.
      if (av != null)
        analyzer.assignPtr(lhsAV, av);
    } else if (lvalue instanceof ArrayIndexExpr) { // x[i] = y 
      // Note - the rhs is either a simple variable or we have to create
      // an alias variable for it.
      // Note - we treat an assignment to an array as a simple assignment.
      if (av != null)
        analyzer.simpleAssign(lhsAV, av);
    } else if (lvalue instanceof SubscriptExpr) { // x[i] = y 
      // Note - the rhs is either a simple variable or we have to create
      // an alias variable for it.
      // Note - we treat an assignment to an array as a simple assignment.
      if (av != null)
        analyzer.simpleAssign(lhsAV, av);
    } else {
      throw new scale.common.InternalError("FindAliases expects simple loads : expr = " +
                                           c +
                                           " lvalue = " +
                                           lvalue);
    }
  }

  /**
   * Handle expressions that may involve pointer values.
   * @param expr the expression
   */
  public void visitExpr(Expr expr)
  {
    pointerExpression(expr);
  }

  //
  // We don't do anything for the routines that follow.  Basically, in
  // these cases, we don't want to default to the generic visitExpr
  // routine.
  //

  public void visitLiteralExpr(LiteralExpr e) 
  {
  }

  public void visitDualExpr(DualExpr e) 
  {
  }

  public void visitSubscriptExpr(SubscriptExpr e) 
  {
  }

  public void visitArrayIndexExpr(ArrayIndexExpr e) 
  {
  }

  /**
   * A general routine that handles (potential) operations involving
   * pointers.  For example, we need to track the addition of two
   * pointer values.  This method creates an alias variable for the
   * generic expression <tt>op(y1,...,yN)</tt> that is not directly
   * stored anywhere. By directly stored, we mean that the LHS is a
   * simple variable and does not involve any indirection.  We need to
   * create an alias variable to hold the result.
   */
  public void pointerExpression(Expr expr)
  {
    Type type = expr.getCoreType();
    if (!type.isPointerType() && !type.isIntegerType())
      return;

    // If the expression isn't assigned or the lhs is not a simple
    // variable reference then we need to create an alias variable.

    Expr lhs = isAssigned(expr);
    if ((lhs == null) || !isSimpleExpr(lhs)) {
      Vector<AliasVar> opAliasVars = new Vector<AliasVar>();
      int              l           = expr.numOperands();

      for (int i = 0; i < l; i++) {
        Expr     op = expr.getOperand(i);
        AliasVar av = getAliasVar(op);
        if (av != null)
          opAliasVars.addElement(av);
      }

      Declaration tdecl   = createNewTemp("pointerExpression", expr.getType(), expr, lhs);
      AliasVar    tempVar = analyzer.addVariable(tdecl);

      aliasesObject.addAliasVarToVector(tempVar);

      analyzer.opAssign(tempVar, opAliasVars);

      // Wait until after call to assign alias annotation (because of getAliasVar).

      AliasAnnote.create(expr, creator, Support.systemTrue, tempVar);
    }
  }
}

