package scale.backend.xyz;

import scale.backend.*;
import scale.common.*;

/** 
 * This class generates Xyz assembly language from a list of Xyz
 * instructions.
 * <p>
 * $Id: XyzAssembler.java,v 1.1 2006-11-16 17:28:18 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class generates the assembly language for this architecture
 * from the instructions for this architecture.  Most of the
 * methods below generate a "not implemented error".  These must be
 * replaced with code that generates the proper assemblylanguage sequence.
 * The other methods may need to be modified.  Use the assembler classes
 * from the other architectures to help you understand how to make
 * changes for your architecture.
 * @see scale.backend.Assembler
 * @see scale.backend.alpha.AlphaAssembler
 * @see scale.backend.sparc.SparcAssembler
 * @see scale.backend.ppc.PPCAssembler
 */

public final class XyzAssembler extends scale.backend.Assembler
{
  private RegisterSet registers;
  private boolean     is;

  /**
   * @param gen is the instruction generator used to generate the instructions.
   * @param source specifies the source program
   */
  public XyzAssembler(Generator gen, String source, boolean instructionScheduling)
  {
    super(gen, source);
    this.registers = gen.getRegisterSet();
    this.is        = instructionScheduling;
  }

  public void assembleProlog(Emit emit)
  {
    throw new scale.common.NotImplementedError("assembleProlog");
  }

  public void assembleEpilog(Emit emit)
  {
    throw new scale.common.NotImplementedError("assembleEpilog");
  }

  public String getLabelString(Label label)
  {
    throw new scale.common.NotImplementedError("getLabelString");
  }

  public void assembleLabel(Label label, Emit emit)
  {
    throw new scale.common.NotImplementedError("assembleLabel");
  }

  public void assembleComment(String comment, Emit emit)
  {
    emit.emit(" # ");
    emit.emit(comment);
  }

  public String assembleRegister(int reg)
  {
    return registers.registerName(reg);
  }


  public void assembleDataAreaHeader(Emit emit, int kind)
  {
    throw new scale.common.NotImplementedError("assembleDataAreaHeader");
  }

  protected void genDirective(Emit emit, int dt)
  {
    throw new scale.common.NotImplementedError("genDirective");
  }


  protected int getDirectiveSize(int dt)
  {
    throw new scale.common.NotImplementedError("getDirectiveSize");
  }

  protected void genZeroFill(Emit emit, long size)
  {
    throw new scale.common.NotImplementedError("genZeroFill");
  }

  protected void genAsciiText(Emit emit, String str)
  {
    throw new scale.common.NotImplementedError("genAsciiText");
  }

  protected void genBytes(Emit emit, int numBytes, long data, int reps)
  {
    throw new scale.common.NotImplementedError("genBytes");
  }

  protected long genData(Emit emit, int dt, Label lab, int reps, boolean aligned)
  {
    throw new scale.common.NotImplementedError("genData");
  }

  public long assembleData(Emit emit, SpaceAllocation sa, long location)
  {
    throw new scale.common.NotImplementedError("assembleData");
  }
}
