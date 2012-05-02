package scale.backend;

/** 
 * This class represents a displacement field in an instruction that 
 * represents a label.
 * <p>
 * $Id: LabelDisplacement.java,v 1.19 2007-09-19 20:55:34 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class LabelDisplacement extends Displacement
{
  private Label label;

  /**
   * Obtain a Displacement for the specified label.
   */
  public LabelDisplacement(Label label)
  {
    super();
    this.label = label;
  }

  /**
   * Return a unique displacement.  Each label reference has a
   * different offset because they are relative to the place of the
   * reference.
   * @see SymbolDisplacement
   */
  public Displacement unique()
  {
    return new LabelDisplacement(label);
  }

  /**
   * Return the label associated with this displacement.
   */
  public Label getLabel()
  {
    return label;
  }
  
  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public String assembler(Assembler asm)
  {
    return asm.getLabelString(label);
  }

  public String toString()
  {
    return "(L " + label.getLabelIndex() + ")";
  }

  /**
   * Return true if the displacements are equivalent.
   */
  public boolean equivalent(Object o)
  {
    if (!super.equivalent(o))
      return false;

    LabelDisplacement d = (LabelDisplacement) o;
    return label.getLabelIndex() == d.label.getLabelIndex();
  }
}
