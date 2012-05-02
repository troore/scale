package scale.backend.x86;

import scale.backend.*;
import scale.common.*;

/** 
 * This class generates X86 assembly language from a list of X86
 * instructions.
 * <p>
 * $Id: X86Assembler.java,v 1.1 2007-11-01 16:52:29 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
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

public final class X86Assembler extends scale.backend.Assembler
{
  private RegisterSet registers;
  private boolean     is;

  /**
   * @param gen is the instruction generator used to generate the instructions.
   * @param source specifies the source program
   */
  public X86Assembler(Generator gen, String source, boolean instructionScheduling)
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

  public void buildAddress(Emit emit, int baseReg, int indexReg, Displacement disp, int scale)
  {
    assert (disp == null) || (baseReg == -1) || (indexReg == -1) || (baseReg == X86RegisterSet.EBP);

    if (disp != null) {
      if (disp.isNumeric()) {
        long offset = disp.getDisplacement();
        if (offset != 0)
          emit.emit(offset);
      } else
        emit.emit(disp.assembler(this));
    }

    if (baseReg != -1) {
      emit.emit('(');
      emit.emit(assembleRegister(baseReg));
      if (indexReg != -1) {
        emit.emit('+');
        if (scale != 1) {
          emit.emit(scale);
          emit.emit('*');
        }
        emit.emit(assembleRegister(indexReg));
      }
      emit.emit(')');
    }
  }

  public void buildAddress(StringBuffer buf, int baseReg, int indexReg, Displacement disp, int scale)
  {
    if (disp != null) {
      if (disp.isNumeric()) {
        long offset = disp.getDisplacement();
        if (offset != 0)
          buf.append(offset);
      } else
        buf.append(disp.assembler(this));
    }

    if (baseReg != -1) {
      buf.append('(');
      buf.append(assembleRegister(baseReg));
      if (indexReg != -1) {
        buf.append('+');
        if (scale != 1) {
          buf.append(scale);
          buf.append('*');
        }
        buf.append(assembleRegister(indexReg));
      }
      buf.append(')');
    }
  }
}
