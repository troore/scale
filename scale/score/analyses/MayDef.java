package scale.score.analyses;

import scale.common.*;
import scale.score.*;
import scale.score.chords.ExprChord;
import scale.score.expr.*;

/**
 * A node to represent <i>MayDef</i> information in the alias SSA form
 * (some people call them preserving defs or weak updates). 
 * <p>
 * $Id: MayDef.java,v 1.6 2007-10-04 19:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The MayDef info represents an assignment that indirectly may change the
 * value of a variable.  However, it may not, which means that the old
 * defintion is still valid.  The May Def operator may only be associated
 * with assignment and call operators.
 * <p>
 * We model the may def information as described in the paper
 * <cite>
 * Effective Representation of Aliases and Indirect Memory 
 * Operations in SSA Form
 * </cite> by Chow et. al.
 *
 * @see MayUse
 */
public class MayDef implements DisplayNode
{
  private static int count = 0;

  private LoadDeclAddressExpr lhs; 
  private LoadDeclValueExpr   rhs; 
  private Note                graphNode; // A pointer back to the Scribble/Score node that we hang this may use information from.


  /**
   * A def->use edge (Expr) or a set of def->use edges (vector).
   */
  private Object defUse; 

  private int id;

  /**
   * Create may def information.  The may def node represents a potential
   * definition of variables due to aliasing.
   *
   * @param lhs the left-hand-side of the may definition (a virtual variable)
   * @param rhs the right-hand-side of the may definition (a virtual variable)
   */
  public MayDef(LoadDeclAddressExpr lhs, LoadDeclValueExpr rhs)
  {
    this.lhs = lhs;
    this.rhs = rhs;
    this.defUse = null;
    id = count++;
  }

  /**
   * Return a deep-copy of this object.
   */
  public MayDef copy()
  {
    MayDef md = new MayDef((LoadDeclAddressExpr) lhs.copy(), (LoadDeclValueExpr) rhs.copy());
    return md;
  }

  /**
   * Get the lhs of the may definition. 
   * @return the lhs of the may definition. 
   */
  public final LoadDeclAddressExpr getLhs()
  {
    return lhs;
  }

  /**
   * Get the rhs of the may definition.  If this is the initial definition
   * then we return null.
   * @return the rhs of the may definition.
   */
  public final LoadDeclValueExpr getRhs()
  {
    return rhs;
  }

  /**
   * Add a new link from the definition to the use.
   * This method DOES NOT update the use to def link in the use.
   * @param expr the new use-def link
   */
  @SuppressWarnings("unchecked")
  public final void addDefUse(MayUse expr)
  {
    if (defUse == null)
      defUse = expr;
    else if (defUse instanceof Vector)
      ((Vector<MayUse>) defUse).addElement(expr);
    else {
      Vector<MayUse> v = new Vector<MayUse>(2);
      v.addElement((MayUse) defUse);
      v.addElement(expr);
      defUse = v;
    }
  }

  /**
   * Remove an existing  link from the definition to the use.
   * This method DOES NOT update the use to def link in the use.
   * @param expr the existing use-def link
   */
  public final void removeDefUse(MayUse expr)
  {
    if (defUse == null)
      return;

    if (defUse == expr) {
      defUse = null;
      return;
    }

    @SuppressWarnings("unchecked")
    Vector<MayUse> v = (Vector<MayUse>) defUse;
    v.removeElement(expr);
  }

  /**
   * Connect the may-use to the node where the use occurs.
   */
  public void setGraphNode(Note graphNode)
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
  public Note getGraphNode()
  {
    return graphNode;
  }

  /**
   * Return the representative name for the operation.
   */
  public String getDisplayLabel()
  {
    return "chi";
  }

  public DColor getDisplayColorHint()
  {
    return DColor.GREEN;
  }

  /**
   * Return true is a def-use link exists.
   */
  public final boolean checkDefUse(MayUse use)
  {
    if (defUse == null)
      return false;

    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<MayUse> v = (Vector<MayUse>) defUse;
      int            l = v.size();
      for (int i = 0; i < l; i++)
        if (use == v.elementAt(i))
          return true;
      return false;
    }

    return (defUse == use);
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
    return "C" + id;
  }
}
