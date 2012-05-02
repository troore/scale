package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 instructions except branches
 * that reference three registers and two displacements.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86RRRDDInstruction extends X86RRRDInstruction
{
  protected Displacement disp2;

  public X86RRRDDInstruction(int          opcode,
                             int          reg,
                             int          reg2,
                             int          reg3,
                             Displacement disp,
                             Displacement disp2)
  {
    super(opcode, reg, reg2, reg3, disp);
    this.disp2 = disp2;
  }

  public Displacement getDisplacement2()
  {
    return disp2;
  }

  public void setDisplacement2(Displacement disp)
  {
    this.disp2 = disp;
  }
}
