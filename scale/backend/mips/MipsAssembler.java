package scale.backend.mips;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.score.*;
import scale.clef.type.*;
import scale.score.chords.*;
import scale.score.expr.*;

/** 
 * This class generates Mips assembly language from a list of Mips
 * instructions.
 * <p>
 * $Id: MipsAssembler.java,v 1.19 2007-10-04 19:57:53 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class MipsAssembler extends scale.backend.Assembler
{
  private Machine     machine;
  private RegisterSet registers;

  /**
   * @param gen is the instruction generator used to generate the instructions.
   * @param source specifies the source program
   */
  public MipsAssembler(Generator gen, String source)
  {
    super(gen, source);
    this.machine   = gen.getMachine();
    this.registers = gen.getRegisterSet();
  }

  /**
   * Called at the very beginning of generating assembly code.
   * Generate assembler directives needed at the beginning of the
   * assembly language file.
   */
  public void assembleProlog(Emit emit)
  {
    emit.emit("\t.set\tnoat"); // Assembly language uses no macros that use the AT register.
    emit.endLine();
    emit.emit("\t.set\tnoreorder"); // The assembler should not reorder instructions.
    emit.endLine();
    emit.emit("\t.file\t1\t\""); // Debugger information
    emit.emit(source);
    emit.emit("\"");
    emit.endLine();
  }

  /**
   * Called at the very end of generating assembly code.
   * Generate assembler directives needed at the end of the assembly language file.
   */
  public void assembleEpilog(Emit emit)
  {
  }

  /**
   * Return the String representing the label.
   */
  public String getLabelString(Label label)
  {
    return "$L" + label.getLabelIndex();
  }

  /**
   * Generate a label in the assembly output.
   */
  public void assembleLabel(Label label, Emit emit)
  {
    emit.endLine();
    emit.emit(getLabelString(label));
    emit.emit(':');
  }

  /**
   * Insert the assembler representation of the comment into the output stream. 
   */
  public void assembleComment(String comment, Emit emit)
  {
    emit.emit("# ");
    emit.emit(comment);
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public String assembleRegister(int reg)
  {
    return registers.registerName(reg);
  }
  
  /**
   * Generate assembler directives for the start of a data area.
   * @param kind specifies the area kind
   */
  public void assembleDataAreaHeader(Emit emit, int kind)
  {
    switch (kind) {
    case MipsGenerator.TEXT:
      emit.emit("\t.section\t.text ");
      emit.endLine();
      emit.emit("\t.align\t4 ");
      emit.endLine();
      return;
    case MipsGenerator.BSS:
    case MipsGenerator.SBSS:
    case MipsGenerator.DATA:
      emit.emit("\t.section\t.data ");
      emit.endLine();
      return;
    case MipsGenerator.LIT4:
      emit.emit("\t.section\t.lit4 ");
      emit.endLine();
      return;
    case MipsGenerator.LIT8:
      emit.emit("\t.section\t.lit8 ");
      emit.endLine();
      return;
    case MipsGenerator.RDATA:
      emit.emit("\t.section\t.rdata ");
      emit.endLine();
      return;
    case MipsGenerator.SDATA:
      emit.emit("\t.section\t.sdata ");
      emit.endLine();
      return;
    default:
      throw new scale.common.InternalError("What area " + kind);
    }
  }

  // NONE   BYTE     SHORT    INT      LONG      FLT       DBL        ADDRESS  TEXT   LDBL
  private static final String[] directives = {
    "???", ".byte", ".half", ".word", ".dword", ".float", ".double", ".word", "???", ".xxxx"};
  private static final int[]    sizes      = {
    0,       1,       2,       4,        8,        4,         8,       4,     0,       0};
  private static final int[]    dtalign    = {
    1,       1,       2,       4,        4,        4,         4,       4,     4,       4};

  /**
   * Generate the assembly directive required for the type.
   * @param dt - the data type
   * @param emit specifies where to put the directive.
   * @see scale.backend.SpaceAllocation
   */
  protected void genDirective(Emit emit, int dt)
  {
    emit.emit('\t');
    emit.emit(directives[dt]);
    emit.emit('\t');
  }

  /**
   * Return the number of addressable units required for one value of the
   * specified type.
   * @param dt - the data type
   * @see scale.backend.SpaceAllocation
   */
  protected int getDirectiveSize(int dt)
  {
    return sizes[dt];
  }

  public long assembleData(Emit emit, SpaceAllocation sa, long location)
  {
    int    vis   = sa.getVisibility();
    int    dt    = sa.getType();
    int    size  = (int) sa.getSize();
    int    type  = sa.getType();
    int    reps  = sa.getReps();
    int    aln   = sa.getAlignment();
    String name  = sa.getName();
    Object value = sa.getValue();

    if (dt == SpaceAllocation.DAT_NONE) {
      if (value != null)
        throw new scale.common.InternalError("DAT_NONE with value " + sa);

      switch (vis) {
      case SpaceAllocation.DAV_LOCAL:  emit.emit("\t.lcomm\t");  break;
      case SpaceAllocation.DAV_GLOBAL: emit.emit("\t.comm\t");   break;
      case SpaceAllocation.DAV_EXTERN: emit.emit("\t.extern\t"); break;
      }
      emit.emit(name);
      emit.emit(' ');
      emit.emit(size);
      emit.endLine();
      return location;
    }

    if (dt == SpaceAllocation.DAT_TEXT) {
      assembleInstructions(emit, name, (Instruction) value);
      return location;
    }

    
    if (name != null) { // Generate the assembler directives to label a data area.
      int nl = (int) Machine.alignTo(location, aln);
      if (nl > location) {
        emit.emit("\t.byte\t0 : ");
        emit.emit(nl - location);
        emit.emit("\t# align");
        emit.endLine();
        location = nl;
      }

      if (vis == SpaceAllocation.DAV_GLOBAL) {
        emit.emit("\t.globl\t");
        emit.emit(name);
        emit.endLine();
      }

      emit.emit(name);
      emit.emit(':');
      emit.endLine();
    }

    long as = genData(emit, dt, value, reps, aln >= dtalign[dt]);
    if (size > as)
      genZeroFill(emit, size - as);

    return location + size;
  }

  /**
   * Generate the data representation for address of the label.
   * @param emit specifies where to generate the data
   * @param dt specifies the data type
   * @param lab is the label
   * @param reps specifies how many times to generate the representation
   * @param aligned specifies whether the data will be aligned
   * @return the number of bytes generated
   * @see scale.backend.SpaceAllocation
   */
  protected long genData(Emit emit, int dt, Label lab, int reps, boolean aligned)
  {
    int    index = lab.getLabelIndex();
    String label = "$L" + index;

    emit.emit("\t.word\t");
    emit.emit(label);

    if (reps > 1) {
      emit.emit(" : ");
      emit.emit(reps);
    }

    emit.endLine();
    return sizes[dt] * reps;
  }
}
