package scale.score.analyses;

import scale.common.*;
import scale.alias.*;
import scale.alias.steensgaard.ECR;
import scale.annot.*;
import scale.clef.decl.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.pred.*;

/**
 * This is the base class for generating information, at specific
 * nodes, which is used to represent aliases and indirect operations
 * in SSA form.
 * <p>
 * $Id: PlaceIndirectOps.java,v 1.56 2007-10-04 19:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class PlaceIndirectOps extends scale.score.pred.TraceChords 
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
   * Alias analysis used. 
   */
  protected Aliases aliases;

  /**
   * Set the trace flag.
   */
  public void setTrace(boolean trace)
  {
    this.trace = (trace && classTrace) || Debug.debug(3);
  }

  /**
   * Construct an object to place special annotations in a Scribble graph
   * so that we can correctly handle indirect operations and aliases.
   */
  public PlaceIndirectOps(Aliases aliases)
  {
    this.aliases = aliases;
  }

  /**
   * Return the virtual variable associated with the ECR or create a new
   * virtual var if there isn't one already.
   * @param ecr an ECR that represents a virtual variable.
   * @return the virtual variable associated with the ECR.
   */
  public VirtualVar getVirtualVar(ECR ecr)
  {
    return aliases.getVirtualVar(ecr);
  }

  /** 
   * Get the alias information from a Scribble expression node.  The
   * alias information is placed on declaration nodes as annotations.
   * The declaration nodes are attached to Scribble load nodes.
   *
   * @param d the declaration node that contains the alias annotation
   * @return the alias variable associated with the declaration.
   */
  protected AliasVar getAliasVar(Declaration d)
  {
    AliasAnnote annote = (AliasAnnote) d.getAnnotation(AliasAnnote.annotationKey());
    return annote.getAliasVar();
  }

  /**
   * Get the alias variable associated with a Scribble operator.  Most Scribble
   * operators do not have alias variables so this routine returns null.
   * Typically, the alias variable information is attached to the declaration
   * node associated with the load operations.  However, we sometimes need to
   * create alias variables to hold alias information that is not directly 
   * assigned to a user variable (e.g., <tt>**x</tt>).
   * <p>
   * For <tt>LoadDeclValueExpr</tt> and <tt>LoadDeclAddressExpr</tt>
   * the information is located in the "extra" information.  For
   * <tt>LoadValueIndirectExpr</tt>, the information is located in the
   * operand.
   *
   * @param exp is the expression representing the operator.
   * @return the alias variable associated with a Scribble operator.
   */
  protected AliasVar getAliasVar(Expr exp)
  {
    // Check if the expression node has the alias info - this means that
    // we've create a 'temporary' variable to hold the alias info.

    AliasAnnote annote = exp.getAliasAnnote();
    if (annote != null)
      return annote.getAliasVar();

    Expr expr = exp.getReference();
    if (expr == null)
      return null;

    annote = expr.getAliasAnnote();
    if (annote != null)
      return annote.getAliasVar();

    if (expr instanceof LoadExpr) {
      Declaration d = ((LoadExpr) expr).getDecl();

      assert (!(d instanceof VariableDecl) ||
              (d.isTemporary() && (d.residency() != Residency.MEMORY))) :
        "Decl (ldv or lda) should have alias var - " + d;

      return aliases.newAliasVariable(d);
    } 

    return null;
  }

  /**
   * Specify that the specified variable access may reference the same
   * location as other references.
   */
  protected void addMayUse(LoadDeclValueExpr le, VirtualVar v)
  {
    MayUse mayUse = new MayUse(v);

    le.addMayUse(mayUse);

    if (trace) {
      System.out.print("Add may use ");
      System.out.print(mayUse);
      System.out.print(" to expr ");
      System.out.println(le);
    }
  }

  /**
   * Specify that the specified variable access may reference the same
   * location as other references.
   */
  protected void addMayUse(LoadDeclValueExpr le, ECR ecr)
  {
    addMayUse(le, aliases.getVirtualVar(ecr));
  }

  /**
   * Specify that the specified variable access may reference the same
   * location as other references.
   */
  protected void addMayUse(LoadValueIndirectExpr le, VirtualVar v)
  {
    MayUse mayUse = new MayUse(v);

    le.addMayUse(mayUse);

    if (trace) {
      System.out.print("Add may use ");
      System.out.print(mayUse);
      System.out.print(" to expr ");
      System.out.println(le);
    }
  }

  /**
   * Specify that the specified variable access may reference the same
   * location as other references.
   */
  protected void addMayUse(LoadValueIndirectExpr le, ECR ecr)
  {
    addMayUse(le, aliases.getVirtualVar(ecr));
  }

  /**
   * Create a may definition expression to repesent the aliasing characteristics
   * of an expression.
   *
   * @param v is the equivalence class that the may def is associated with
   * @return the may definition expression
   */
  protected MayDef createMayDefInfo(VirtualVar v)
  {
    LoadDeclValueExpr   rhs    = new LoadDeclValueExpr(v);
    LoadDeclAddressExpr lhs    = new LoadDeclAddressExpr(v);
    MayDef          mayDef = new MayDef(lhs, rhs);

    return mayDef;
  }
  /**
   * Create a may definition expression to repesent the aliasing characteristics
   * of an expression.
   *
   * @param ecr the equivalence class that the may def is associated with
   * @return the may definition expression
   */
  protected MayDef createMayDefInfo(ECR ecr)
  {
    assert (ecr != null) : "No ECR for the variable.";

    return createMayDefInfo(aliases.getVirtualVar(ecr));
  }
}
