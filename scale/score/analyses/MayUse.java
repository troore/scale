package scale.score.analyses;

import scale.common.Stack;

import scale.common.*;
import scale.score.*;
import scale.clef.decl.*;
import scale.score.expr.*;

/**
 * A node to represent <i>MayUse</i> information in the alias SSA form.  
 * <p>
 * $Id: MayUse.java,v 1.7 2007-10-04 19:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The may use node represents a potential use of a
 * variable.  May use information is associated with variable
 * references.  We create virtual variables to describe the 
 * potential variables used in the may use.
 * <p>
 * We model the may use information as described in the paper
 * <cite>
 * Effective Representation of Aliases and Indirect Memory 
 * Operations in SSA Form
 * </cite> by Chow et. al.
 * 
 * @see MayDef
 */
public final class MayUse implements DisplayNode
{
  private static int count = 0;

  /**
   * The declaration for the value loaded - if any.
   */
  private VirtualVar decl;

  /**
   * Use->def link.
   */
  private MayDef mayDef;

  /**
   * May use information - due to aliasing.
   */
  private MayUse mayUse;

  /**
   * A pointer back to the Scribble/Score node that we hang this
   * may use information from.
   */
  private Expr graphNode = null;

  private int id;

  /**
   * Create may use information.  The may use node represents the
   * potential reference used by a variable reference due to aliasing.
   * @param decl a declaration for the virtual variable
   */
  public MayUse(VirtualVar decl)
  {
    this.decl = decl;
    id = count++;
  }

  /**
   * Make a copy of this expression.
   * The use - def information is copied too.
   */
  public MayUse copy(Expr graphNode)
  {
    MayUse ld = new MayUse(decl);
    ld.mayDef = mayDef;
    return ld;
  }

  /**
   * Define a link from this load of a value to the
   * scale.score.chords.ExprChord that defines the value.
   * @param expr the new use-def link
   */
  public void setMayDef(MayDef expr)
  {
    if (mayDef != null)
      mayDef.removeDefUse(this);
    mayDef = expr;
    if (mayDef != null)
      mayDef.addDefUse(this);
  }

  /**
   * Return the {@link scale.score.chords.ExprChord ExprChord} that defines
   * the value load by this load expression.
   */
  public MayDef getMayDef()
  {
    return mayDef;
  }

  /**
   * Add may use information to the load expression.
   * @param mayUse the expresion representing the may use
   */
  public void addMayUse(MayUse mayUse)
  {
    assert ((this.mayUse == mayUse) ||
            (this.mayUse == null) ||
            (mayUse == null)) :
      "Resetting may-use information " + this;

    this.mayUse = mayUse;
  }

  /**
   * Return the may use information assocaited with the load.
   */
  public final MayUse getMayUse()
  {
    return mayUse;
  }

  /**
   * Return the Clef declaration of the variable being loaded.
   */
  public final VirtualVar getDecl()
  {
    return decl;
  }

  /**
   * Return the Clef declaration of the variable being loaded.
   */
  public final void setDecl(VariableDecl decl)
  {
    this.decl = (VirtualVar) decl;
  }

  /**
   * Change the declaration associated with the load operation.
   * @param decl is the new declataion
   */
  public final void setDecl(VirtualVar decl)
  {
    this.decl = decl;
  }

  /**
   * Return the name of the variable declaration.
   */
  public String getName()
  {
    if (decl == null)
      return "??";
    return decl.getName();
  }

  /**
   * Return a deep-copy of this object.
   * This form of copy() not allowed.
   */
  public Expr copy()
  {
    throw new scale.common.InternalError("This form of copy() not allowed on " + this);
  }

  /**
   * Connect the may-use to the node where the use occurs.
   */
  public void setGraphNode(Expr graphNode)
  {
    assert ((this.graphNode == graphNode) || (this.graphNode == null)) :
      "Changing graph node link.";
    this.graphNode = graphNode;
  }

  /**
   * Return the scribble/score node that represents the real 
   * use of the may use information.
   * @return the scribble/score node that points to this info.
   */
  public Expr getGraphNode()
  {
    return graphNode;
  }

  /**
   * Return the original declaration of the value loaded.  This is the
   * variable before SSA renumbering is used to determine whether two
   * virtual vars are in the same subset or if one is a
   * superset. Hence the name.
   */ 
  public Declaration getSubsetDecl()
  {
    return decl.getOriginal();
  }

  /**
   * Find the use-def link that matches the virtual variable.
   * May return null.
   */
  public MayDef findMayDef()
  {
    if (mayDef == null)
      return null;

    MayDef ud = mayDef;
    VirtualVar subsetDecl = (VirtualVar) getSubsetDecl();
    do {
      LoadExpr   le = ud.getLhs();
      VirtualVar vv = (VirtualVar) le.getSubsetDecl();
      if (vv.subsetEquiv(subsetDecl))
        break;

      LoadDeclValueExpr rhs = ud.getRhs();
      ud = rhs.getUseDef().getMayDef();
    } while (ud != null);

    // ud may be a store of a phi function so it may not be a MayDef.

    return ud;
  }

  public DColor getDisplayColorHint()
  {
    return DColor.PINK;
  }

  /**
   * Return the representative name for the operation.
   * @return the representative name for the operation.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("(mu ");
    buf.append(decl.getName());
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.  This method should be over-ridden as it
   * simplay returns the shape "box".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }

  /**
   * Return a unique label for graphical displays.
   */
  public String getDisplayName()
  {
    return "M" + id;
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    if (mayDef == null)
      return;

    if (!mayDef.checkDefUse(this))
      throw new scale.common.InternalError("Incorrect use-def link " +
                                           this +
                                           " -> " +
                                           mayDef);
  }
}
