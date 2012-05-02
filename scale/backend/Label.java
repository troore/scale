package scale.backend;

import scale.common.*;

/** 
 * This class marks the position of a point branched to.
 * <p>
 * $Id: Label.java,v 1.31 2007-10-04 19:57:49 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class Label extends Marker
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.Label", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  private Vector<Instruction> predecessors; // A vector of the instructions that branch to this marker.
  private int     label;               // The index of this label.
  private int     strength;            // The importance of this instruction sequence - e.g., the number of times executed.
  private boolean referenced;          // True if the label is referenced.
  private boolean isFirstInBasicBlock; // True if the label is marks the point in the CFG of the start of a basic block.

  /**
   * Create a label.
   * @param referenced is true if this label is the explicit target of a branch.
   * Implicit targets are usually the next contiguous instruction.
   */
  public Label(boolean referenced)
  {
    super();
    this.label               = 0;
    this.referenced          = referenced;
    this.isFirstInBasicBlock = false;
    createdCount++;
  }


  /**
   * Create a label. The label is assumed to be referenced.
   */
  public Label()
  {
    this(true);
  }

  /**
   * Indicate that the label has a reference that is not "fall-through".
   */
  public void setReferenced()
  {
    referenced = true;
  }

  /**
   * Return true if the label is referenced.
   */
  public boolean isReferenced()
  {
    return referenced;
  }

  /**
   * Indicate that the label has no reference that is not "fall-through".
   */
  public void setNotReferenced()
  {
    referenced = false;
  }

  /**
   * Specify that this label marks the point at which two or more CFG
   * edges merge together.  A basic block in the backend is slightly
   * different from a basic block in prior phases of the compiler in
   * that a new basic block starts at the return from a subroutine
   * call.
   */
  public final void markAsFirstInBasicBlock()
  {
    isFirstInBasicBlock = true;
  }

  /**
   * Return true if this label marks the point at which two or more
   * CFG edges merge together.  A basic block in the backend is
   * slightly different from a basic block in prior phases of the
   * compiler in that a new basic block starts at the return from a
   * subroutine call.
   */
  public final boolean isFirstInBasicBlock()
  {
    return isFirstInBasicBlock;
  }

  /**
   * Remove all the predecessors of a label.
   */
  protected void removePredecessors()
  {
    predecessors = null;
  }

  /**
   * Specify an instruction that precedes this label in the execution
   * of the program
   */
  protected void addPredecessor(Instruction inst)
  {
   if (predecessors == null) {
      predecessors = new Vector<Instruction>(1);
    }
    predecessors.addElement(inst);
  }

  /**
   * Replace an instruction that preceded this label in the execution
   * of the program by another instruction
   */
  protected void replacePredecessor(Instruction old, Instruction rep)
  {
    int l = predecessors.size();
    for (int i = 0; i < l; i++) {
      if (old == predecessors.elementAt(i)) {
        predecessors.setElementAt(rep, i);
        return;
      }
    }
    throw new scale.common.InternalError("Predecessor not found " + old);
  }

  /**
   * Return the number of edges into this label.
   */
  public int numPredecessors()
  {
    if (predecessors == null)
      return 0;
    return predecessors.size();
  }

  /**
   * Return the specified predecessor.
   */
  public Instruction getPredecessor(int i)
  {
    return predecessors.elementAt(i);
  }

  /**
   * Return true if this is a label marker.
   */
  public final boolean isLabel()
  {
    return true;
  }

  /**
   * Return the integer value associated with this label.
   */
  public final int getLabelIndex()
  {
    return label;
  }

  /**
   * Set the integer value associated with this label.
   */
  public final void setLabelIndex(int labelIndex)
  {
    label = labelIndex;
  }

  /**
   * Return the strength value associated with this label.
   */
  public final int getStrength()
  {
    return strength;
  }

  /**
   * Set the strength value associated with this label.
   */
  public final void setStrength(int strength)
  {
    this.strength = strength;
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (!referenced)
      return;

    asm.assembleLabel(this, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("Label: ");
    buf.append(label);
    buf.append(" #");
    buf.append(strength);
    buf.append(referenced ? " referenced" : "");
    buf.append(isFirstInBasicBlock ? " firstInBasicBlock" : "");
    return buf.toString();
  }
}
