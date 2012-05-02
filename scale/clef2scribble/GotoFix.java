package scale.clef2scribble;

import scale.common.Stack;
import scale.common.*;
import scale.score.chords.*;

/**
 * This class is used to record forward gotos.
 * <p>
 * $Id: GotoFix.java,v 1.17 2007-10-04 19:58:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This record is a GotoChord, LabelDecl pair.
 * The LabelDecl is used to find the actual score node to which the label refers.
 * The target of the GotoChord is then set.
 */
public class GotoFix 
{
  private Stack<Object>   gs;     // Statement that needs cfg out edge, Label of out edge statement, For DecisionChord - index of out edge.
  private HashMap<Object, Chord> labels; // Map from label to Chord.

  public GotoFix()
  {
    gs     = new Stack<Object>();
    labels = new HashMap<Object, Chord>(203);
  }

  /**
   * Record a new forward reference.
   * @param bc is the branch Chord
   * @param label is the label branched to
   * @param index is the index of the out-going CFG edge of the branch Chord
   */
  public void add(Chord bc, Object label, Object index)
  {
    gs.push(bc);
    gs.push(label);
    gs.push(index);
  }

  /**
   * Associate a label with a Chord.
   */
  public void defineLabel(Object label, Chord s)
  {
    labels.put(label, s);
  }

  /**
   * Return the Chord associated with this label.
   */
  public Chord getChord(Object label)
  {
    return labels.get(label);
  }

  /**
   * Fix whatever forward gotos there were.
   */
  public void fixupGotos()
  {
    while (!gs.empty()) { // for each forward goto
      Object index = gs.pop();
      Object label = gs.pop();
      Chord  fgs   = (Chord) gs.pop();
      Chord  dest  = labels.get(label);  // find the label
      assert (dest != null) : "Goto without a label " + label;
      if (fgs instanceof BranchChord) {
        BranchChord bgs = (BranchChord) fgs;
        bgs.setTarget(dest); // set the target of the goto to the labeled statement
      } else if (fgs instanceof SwitchChord) {
        SwitchChord ds = (SwitchChord) fgs;
        ds.addBranchEdge(index, dest);
      } else if (fgs instanceof IfThenElseChord) {
        IfThenElseChord ifChord = (IfThenElseChord) fgs;
        boolean         t       = ((Boolean) index).booleanValue();
        if (t)
          ifChord.setTrueEdge(dest);
        else
          ifChord.setFalseEdge(dest);
      } else if (fgs.isSequential()) {
        fgs.linkTo(dest);
      } else
        throw new scale.common.InternalError("Not a control-transfer statement " + fgs);
    }
  }
}

